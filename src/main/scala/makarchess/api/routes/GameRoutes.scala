package makarchess.api.routes

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.DecodingFailure
import makarchess.api.auth.AuthVerifier
import makarchess.api.dto.*
import makarchess.api.json.JsonCodecs.given
import makarchess.api.service.{ApiError, ApiGameService}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Authorization

final class GameRoutes[F[_]: Concurrent](service: ApiGameService, authVerifier: AuthVerifier) extends Http4sDsl[F]:
  private given org.http4s.EntityDecoder[F, GameConfigRequest] = jsonOf[F, GameConfigRequest]
  private given org.http4s.EntityDecoder[F, FenRequest] = jsonOf[F, FenRequest]
  private given org.http4s.EntityDecoder[F, PgnRequest] = jsonOf[F, PgnRequest]
  private given org.http4s.EntityDecoder[F, MoveRequest] = jsonOf[F, MoveRequest]
  private given org.http4s.EntityEncoder[F, ErrorResponse] = jsonEncoderOf[F, ErrorResponse]
  private given org.http4s.EntityEncoder[F, HealthResponse] = jsonEncoderOf[F, HealthResponse]
  private given org.http4s.EntityEncoder[F, GameStateResponse] = jsonEncoderOf[F, GameStateResponse]
  private given org.http4s.EntityEncoder[F, GameStatusResponse] = jsonEncoderOf[F, GameStatusResponse]
  private given org.http4s.EntityEncoder[F, ReplayStatusResponse] = jsonEncoderOf[F, ReplayStatusResponse]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "health" =>
      Ok(HealthResponse("ok"))

    case req @ POST -> Root / "game" / "new" =>
      withAuthenticatedUser(req) { userId =>
        if req.contentLength.forall(_ == 0L) then respond(service.newGame(userId, GameConfigRequest()))
        else
          req.attemptAs[GameConfigRequest].value.flatMap {
            case Right(config) => respond(service.newGame(userId, config))
            case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
            case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
          }
      }

    case req @ POST -> Root / "game" / "reset" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.resetGame(userId))
      }

    case req @ GET -> Root / "game" / "board" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.currentBoard(userId))
      }

    case req @ GET -> Root / "game" / "status" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.currentStatus(userId))
      }

    case req @ GET -> Root / "game" / "replay" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.replayStatus(userId))
      }

    case req @ POST -> Root / "game" / "fen" =>
      withAuthenticatedUser(req) { userId =>
        req.attemptAs[FenRequest].value.flatMap {
          case Right(fenRequest) => respond(service.loadFen(userId, fenRequest))
          case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
          case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
        }
      }

    case req @ POST -> Root / "game" / "pgn" =>
      withAuthenticatedUser(req) { userId =>
        req.attemptAs[PgnRequest].value.flatMap {
          case Right(pgnRequest) => respond(service.loadPgnReplay(userId, pgnRequest))
          case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
          case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
        }
      }

    case req @ POST -> Root / "game" / "replay" / "forward" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.replayForward(userId))
      }

    case req @ POST -> Root / "game" / "replay" / "backward" =>
      withAuthenticatedUser(req) { userId =>
        respond(service.replayBackward(userId))
      }

    case req @ POST -> Root / "game" / "move" =>
      withAuthenticatedUser(req) { userId =>
        req.attemptAs[MoveRequest].value.flatMap {
          case Right(moveRequest) => respond(service.makeMove(userId, moveRequest.uci))
          case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
          case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
        }
      }
  }

  private def withAuthenticatedUser[A](req: org.http4s.Request[F])(action: String => F[org.http4s.Response[F]]) =
    extractBearerToken(req) match
      case Left(message) => org.http4s.Response[F](status = org.http4s.Status.Unauthorized).withEntity(ErrorResponse(message)).pure[F]
      case Right(token) =>
        authVerifier.verifyIdToken(token) match
          case Right(user) => action(user.userId)
          case Left(message) => org.http4s.Response[F](status = org.http4s.Status.Unauthorized).withEntity(ErrorResponse(message)).pure[F]

  private def extractBearerToken(req: org.http4s.Request[F]): Either[String, String] =
    req.headers.get[Authorization] match
      case None => Left("Authorization bearer token is required.")
      case Some(Authorization(org.http4s.Credentials.Token(org.http4s.AuthScheme.Bearer, token))) =>
        val trimmed = token.trim
        if trimmed.isEmpty then Left("Authorization bearer token is required.")
        else Right(trimmed)
      case Some(_) => Left("Authorization header must use Bearer token format.")

  private def respond[A](result: Either[ApiError, A])(using encoder: org.http4s.EntityEncoder[F, A]) =
    result match
      case Right(value) => Ok(value)
      case Left(ApiError.BadRequest(message)) => BadRequest(ErrorResponse(message))
      case Left(ApiError.Conflict(message))   => Conflict(ErrorResponse(message))
      case Left(ApiError.Internal(message))   => InternalServerError(ErrorResponse(message))
