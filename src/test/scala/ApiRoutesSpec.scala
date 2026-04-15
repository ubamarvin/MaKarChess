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

  test("POST /game/fen loads a FEN position") {
    val request = Request[IO](POST, uri = uri"/game/fen").withEntity(
      parse("""{"fen":"4k3/8/8/8/8/8/8/4K3 w - - 0 1"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Ok)
        assertEquals(json.hcursor.downField("board").values.map(_.size), Some(2))
        assertEquals(json.hcursor.get[String]("sideToMove"), Right("White"))
      }
    }
  }

  test("POST /game/fen rejects invalid FEN") {
    val request = Request[IO](POST, uri = uri"/game/fen").withEntity(
      parse("""{"fen":"not-a-fen"}""").toOption.get
    )

    app.run(request).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.BadRequest)
        assert(json.hcursor.get[String]("message").isRight)
      }
    }
  }

  test("POST /game/pgn loads replay at the start position and GET /game/replay reports metadata") {
    val api = app
    val request = Request[IO](POST, uri = uri"/game/pgn").withEntity(
      parse("""{"pgn":"1. e4 e5 2. Nf3 Nc6"}""").toOption.get
    )

    for
      loadResponse <- api.run(request)
      loadJson <- loadResponse.as[Json]
      replayResponse <- api.run(Request[IO](GET, uri = uri"/game/replay"))
      replayJson <- replayResponse.as[Json]
    yield
      assertEquals(loadResponse.status, Status.Ok)
      assertEquals(loadJson.hcursor.get[String]("sideToMove"), Right("White"))
      assertEquals(replayResponse.status, Status.Ok)
      assertEquals(replayJson.hcursor.get[Boolean]("active"), Right(true))
      assertEquals(replayJson.hcursor.get[Int]("index"), Right(0))
      assertEquals(replayJson.hcursor.get[Int]("length"), Right(4))
  }

  test("POST /game/replay/forward and backward step through an active replay") {
    val api = app
    val load = Request[IO](POST, uri = uri"/game/pgn").withEntity(
      parse("""{"pgn":"1. e4 e5"}""").toOption.get
    )
    val forward = Request[IO](POST, uri = uri"/game/replay/forward")
    val backward = Request[IO](POST, uri = uri"/game/replay/backward")

    for
      _ <- api.run(load)
      forwardResponse <- api.run(forward)
      forwardJson <- forwardResponse.as[Json]
      backwardResponse <- api.run(backward)
      backwardJson <- backwardResponse.as[Json]
    yield
      assertEquals(forwardResponse.status, Status.Ok)
      assertEquals(forwardJson.hcursor.get[String]("sideToMove"), Right("Black"))
      assertEquals(backwardResponse.status, Status.Ok)
      assertEquals(backwardJson.hcursor.get[String]("sideToMove"), Right("White"))
  }

  test("POST /game/replay/start and end jump to replay boundaries") {
    val api = app
    val load = Request[IO](POST, uri = uri"/game/pgn").withEntity(
      parse("""{"pgn":"1. e4 e5 2. Nf3 Nc6"}""").toOption.get
    )
    val end = Request[IO](POST, uri = uri"/game/replay/end")
    val start = Request[IO](POST, uri = uri"/game/replay/start")

    for
      _ <- api.run(load)
      endResponse <- api.run(end)
      endJson <- endResponse.as[Json]
      startResponse <- api.run(start)
      startJson <- startResponse.as[Json]
    yield
      assertEquals(endResponse.status, Status.Ok)
      assertEquals(endJson.hcursor.get[String]("sideToMove"), Right("White"))
      assertEquals(startResponse.status, Status.Ok)
      assertEquals(startJson.hcursor.get[String]("sideToMove"), Right("White"))
  }

  test("POST /game/replay/forward without active replay returns 409") {
    app.run(Request[IO](POST, uri = uri"/game/replay/forward")).flatMap { response =>
      response.as[Json].map { json =>
        assertEquals(response.status, Status.Conflict)
        assertEquals(json.hcursor.get[String]("message"), Right("No PGN replay loaded."))
      }
    }
  }

  test("normal move clears replay state") {
    val api = app
    val load = Request[IO](POST, uri = uri"/game/pgn").withEntity(
      parse("""{"pgn":"1. e4 e5"}""").toOption.get
    )
    val move = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"e2e4"}""").toOption.get)
    val replay = Request[IO](GET, uri = uri"/game/replay")

    for
      _ <- api.run(load)
      _ <- api.run(move)
      replayResponse <- api.run(replay)
      replayJson <- replayResponse.as[Json]
    yield
      assertEquals(replayResponse.status, Status.Ok)
      assertEquals(replayJson.hcursor.get[Boolean]("active"), Right(false))
  }

  test("GET /game/status returns move history and current PGN") {
    val api = app
    val move = Request[IO](POST, uri = uri"/game/move").withEntity(parse("""{"uci":"e2e4"}""").toOption.get)
    val status = Request[IO](GET, uri = uri"/game/status")

    for
      _ <- api.run(move)
      response <- api.run(status)
      json <- response.as[Json]
    yield
      assertEquals(response.status, Status.Ok)
      assertEquals(json.hcursor.get[String]("currentPgn"), Right("1. e4"))
      assertEquals(json.hcursor.get[List[String]]("moveHistory"), Right(List("e4")))
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
