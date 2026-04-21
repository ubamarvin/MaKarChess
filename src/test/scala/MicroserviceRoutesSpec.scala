import cats.effect.IO
import cats.syntax.all.*
import io.circe.Json
import io.circe.parser.parse
import makarchess.api.routes.{AnalysisRoutes, BotRoutes, GameRoutes, RankingRoutes}
import makarchess.api.service.{ApiAnalysisService, ApiBotService, ApiGameService, ApiRankingService, GameRegistry}
import munit.CatsEffectSuite
import org.http4s.Method.{GET, POST}
import org.http4s.Request
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits.uri

class MicroserviceRoutesSpec extends CatsEffectSuite:

  private def app =
    val registry = new GameRegistry()
    val gameService = new ApiGameService(registry)
    val botService = new ApiBotService(registry)
    val analysisService = new ApiAnalysisService(registry)
    val rankingService = new ApiRankingService()
    (
      new GameRoutes[IO](gameService).routes <+>
        new BotRoutes[IO](botService).routes <+>
        new AnalysisRoutes[IO](analysisService).routes <+>
        new RankingRoutes[IO](rankingService).routes
    ).orNotFound

  test("GET /bot/types lists available chess bots") {
    app.run(Request[IO](GET, uri = uri"/bot/types")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assert(json.hcursor.downField("types").as[List[String]].toOption.exists(_.contains("greedy")))
      }
    }
  }

  test("POST /bot/move returns a legal bot move for the current game") {
    val request = Request[IO](POST, uri = uri"/bot/move").withEntity(
      parse("""{"botType":"greedy"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        val uci = json.hcursor.get[String]("uci").toOption
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("botType"), Right("greedy"))
        assertEquals(json.hcursor.get[Int]("legalMoves"), Right(20))
        assert(uci.exists(move => move.length == 4 || move.length == 5))
      }
    }
  }

  test("GET /analysis/current returns material and mobility for the current game") {
    app.run(Request[IO](GET, uri = uri"/analysis/current")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
        assertEquals(json.hcursor.get[Int]("legalMoves"), Right(20))
        assertEquals(json.hcursor.downField("material").get[Int]("balanceForWhite"), Right(0))
      }
    }
  }

  test("POST /analysis analyzes a provided FEN without changing the current game") {
    val request = Request[IO](POST, uri = uri"/analysis").withEntity(
      parse("""{"fen":"4k3/8/8/8/8/8/8/4K3 w - - 0 1"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
        assertEquals(json.hcursor.downField("material").get[Int]("white"), Right(0))
        assertEquals(json.hcursor.downField("material").get[Int]("black"), Right(0))
      }
    }
  }

  test("POST /ranking/result records a result and updates the leaderboard") {
    val api = app
    val result = Request[IO](POST, uri = uri"/ranking/result").withEntity(
      parse("""{"whitePlayer":"Ada","blackPlayer":"Ben","result":"white"}""").toOption.get
    )

    for
      resultResponse <- api.run(result)
      resultJson <- resultResponse.as[Json]
      rankingResponse <- api.run(Request[IO](GET, uri = uri"/ranking"))
      rankingJson <- rankingResponse.as[Json]
    yield
      assertEquals(resultResponse.status, Status.Ok)
      assertEquals(resultJson.hcursor.downField("white").get[Int]("wins"), Right(1))
      assertEquals(resultJson.hcursor.downField("black").get[Int]("losses"), Right(1))
      assertEquals(rankingResponse.status, Status.Ok)
      assertEquals(rankingJson.hcursor.downField("entries").downArray.get[String]("player"), Right("Ada"))
  }
