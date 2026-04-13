import cats.effect.IO
import io.circe.Json
import io.circe.parser.parse
import makarchess.api.routes.GameRoutes
import makarchess.api.service.{ApiGameService, GameRegistry}
import munit.CatsEffectSuite
import org.http4s.Method.{GET, POST}
import org.http4s.Request
import org.http4s.Status
import org.http4s.circe.CirceEntityCodec._
import org.http4s.implicits.uri

class ApiRoutesSpec extends CatsEffectSuite:

  private def app =
    val service = new ApiGameService(new GameRegistry())
    new GameRoutes[IO](service).routes.orNotFound

  test("GET /health returns ok") {
    app.run(Request[IO](GET, uri = uri"/health")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("status"), Right("ok"))
      }
    }
  }

  test("POST /game/new returns initial game state") {
    app.run(Request[IO](POST, uri = uri"/game/new")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
        assertEquals(json.hcursor.downField("board").values.map(_.size), Some(32))
      }
    }
  }

  test("POST /game/new accepts bot and opponent-model options") {
    val request = Request[IO](POST, uri = uri"/game/new").withEntity(
      parse("""{"botType":"random","botPlays":"black","modeledSide":"white"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
        assertEquals(json.hcursor.get[Boolean]("isCheck"), Right(false))
      }
    }
  }

  test("POST /game/new rejects invalid configured colors") {
    val request = Request[IO](POST, uri = uri"/game/new").withEntity(
      parse("""{"botPlays":"green"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.BadRequest)
        assertEquals(json.hcursor.get[String]("message"), Right("Invalid botPlays value: green. Use 'white' or 'black'."))
      }
    }
  }

  test("POST /game/new rejects unknown bot types") {
    val request = Request[IO](POST, uri = uri"/game/new").withEntity(
      parse("""{"botType":"wizard","botPlays":"black"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.BadRequest)
        assertEquals(json.hcursor.get[String]("message"), Right("Unknown bot type: wizard"))
      }
    }
  }

  test("GET /game/board returns structured board state") {
    app.run(Request[IO](GET, uri = uri"/game/board")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[Int]("fullmoveNumber"), Right(1))
        assertEquals(json.hcursor.get[Boolean]("isCheck"), Right(false))
      }
    }
  }

  test("POST /game/move applies a legal move and returns updated state") {
    val request = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"e2e4"}""").toOption.get)
    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("Black"))
        assertEquals(json.hcursor.get[Int]("fullmoveNumber"), Right(1))
      }
    }
  }

  test("POST /game/move with invalid syntax returns 400") {
    val request = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"bad"}""").toOption.get)
    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.BadRequest)
        assertEquals(json.hcursor.get[String]("message"), Right("Invalid move format. Use e2e4 or e7e8q."))
      }
    }
  }

  test("POST /game/move with illegal move returns 409") {
    val request = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"e2e5"}""").toOption.get)
    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Conflict)
        assertEquals(json.hcursor.get[String]("message"), Right("Illegal move."))
      }
    }
  }

  test("POST /game/reset resets the game") {
    val move = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"e2e4"}""").toOption.get)
    val reset = Request[IO](POST, uri = uri"/game/reset")

    for
      _ <- app.run(move)
      response <- app.run(reset)
      json <- response.as[Json]
    yield
      assertEquals(response.status, Status.Ok)
      assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
      assertEquals(json.hcursor.get[Int]("fullmoveNumber"), Right(1))
  }
