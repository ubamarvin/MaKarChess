package makarchess.api.routes

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.DecodingFailure
import makarchess.api.dto.*
import makarchess.api.json.JsonCodecs.given
import makarchess.api.service.{ApiError, ApiGameService}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

final class GameRoutes[F[_]: Concurrent](service: ApiGameService) extends Http4sDsl[F]:
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
      if req.contentLength.forall(_ == 0L) then respond(service.newGame(GameConfigRequest()))
      else
        req.attemptAs[GameConfigRequest].value.flatMap {
          case Right(config) => respond(service.newGame(config))
          case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
          case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
        }

    case POST -> Root / "game" / "reset" =>
      respond(service.resetGame())

    case GET -> Root / "game" / "board" =>
      respond(service.currentBoard())

    case GET -> Root / "game" / "status" =>
      respond(service.currentStatus())

    case GET -> Root / "game" / "replay" =>
      respond(service.replayStatus())

    case req @ POST -> Root / "game" / "fen" =>
      req.attemptAs[FenRequest].value.flatMap {
        case Right(fenRequest) => respond(service.loadFen(fenRequest))
        case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
        case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
      }

    case req @ POST -> Root / "game" / "pgn" =>
      req.attemptAs[PgnRequest].value.flatMap {
        case Right(pgnRequest) => respond(service.loadPgnReplay(pgnRequest))
        case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
        case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
      }

    case POST -> Root / "game" / "replay" / "forward" =>
      respond(service.replayForward())

    case POST -> Root / "game" / "replay" / "backward" =>
      respond(service.replayBackward())

    case req @ POST -> Root / "game" / "move" =>
      req.attemptAs[MoveRequest].value.flatMap {
        case Right(moveRequest) => respond(service.makeMove(moveRequest.uci))
        case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
        case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
      }
  }

  private def respond[A](result: Either[ApiError, A])(using encoder: org.http4s.EntityEncoder[F, A]) =
    result match
      case Right(value) => Ok(value)
      case Left(ApiError.BadRequest(message)) => BadRequest(ErrorResponse(message))
      case Left(ApiError.Conflict(message))   => Conflict(ErrorResponse(message))
      case Left(ApiError.Internal(message))   => InternalServerError(ErrorResponse(message))
