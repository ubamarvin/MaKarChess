package makarchess.api.routes

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.DecodingFailure
import makarchess.api.dto.*
import makarchess.api.json.JsonCodecs.given
import makarchess.api.service.{ApiAnalysisService, ApiError}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

final class AnalysisRoutes[F[_]: Concurrent](service: ApiAnalysisService) extends Http4sDsl[F]:
  private given org.http4s.EntityDecoder[F, AnalysisRequest] = jsonOf[F, AnalysisRequest]
  private given org.http4s.EntityEncoder[F, AnalysisResponse] = jsonEncoderOf[F, AnalysisResponse]
  private given org.http4s.EntityEncoder[F, ErrorResponse] = jsonEncoderOf[F, ErrorResponse]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "analysis" / "current" =>
      respond(service.analyzeCurrent())

    case req @ POST -> Root / "analysis" =>
      req.attemptAs[AnalysisRequest].value.flatMap {
        case Right(request) => respond(service.analyze(request))
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
