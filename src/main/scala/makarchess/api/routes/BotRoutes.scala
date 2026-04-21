package makarchess.api.routes

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.DecodingFailure
import makarchess.api.dto.*
import makarchess.api.json.JsonCodecs.given
import makarchess.api.service.{ApiBotService, ApiError}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

final class BotRoutes[F[_]: Concurrent](service: ApiBotService) extends Http4sDsl[F]:
  private given org.http4s.EntityDecoder[F, BotMoveRequest] = jsonOf[F, BotMoveRequest]
  private given org.http4s.EntityEncoder[F, BotTypesResponse] = jsonEncoderOf[F, BotTypesResponse]
  private given org.http4s.EntityEncoder[F, BotMoveResponse] = jsonEncoderOf[F, BotMoveResponse]
  private given org.http4s.EntityEncoder[F, ErrorResponse] = jsonEncoderOf[F, ErrorResponse]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "bot" / "types" =>
      Ok(service.availableBots())

    case req @ POST -> Root / "bot" / "move" =>
      req.attemptAs[BotMoveRequest].value.flatMap {
        case Right(request) => respond(service.chooseMove(request))
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
