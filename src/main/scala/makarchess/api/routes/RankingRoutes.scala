package makarchess.api.routes

import cats.effect.Concurrent
import cats.syntax.all.*
import io.circe.DecodingFailure
import makarchess.api.dto.*
import makarchess.api.json.JsonCodecs.given
import makarchess.api.service.{ApiError, ApiRankingService}
import org.http4s.HttpRoutes
import org.http4s.circe.{jsonEncoderOf, jsonOf}
import org.http4s.dsl.Http4sDsl

final class RankingRoutes[F[_]: Concurrent](service: ApiRankingService) extends Http4sDsl[F]:
  private given org.http4s.EntityDecoder[F, RankingPlayerRequest] = jsonOf[F, RankingPlayerRequest]
  private given org.http4s.EntityDecoder[F, RankingResultRequest] = jsonOf[F, RankingResultRequest]
  private given org.http4s.EntityEncoder[F, RankingEntryResponse] = jsonEncoderOf[F, RankingEntryResponse]
  private given org.http4s.EntityEncoder[F, RankingResponse] = jsonEncoderOf[F, RankingResponse]
  private given org.http4s.EntityEncoder[F, RankingResultResponse] = jsonEncoderOf[F, RankingResultResponse]
  private given org.http4s.EntityEncoder[F, ErrorResponse] = jsonEncoderOf[F, ErrorResponse]

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case GET -> Root / "ranking" =>
      Ok(service.leaderboard())

    case req @ POST -> Root / "ranking" / "player" =>
      req.attemptAs[RankingPlayerRequest].value.flatMap {
        case Right(request) => respond(service.addPlayer(request))
        case Left(_: DecodingFailure) => BadRequest(ErrorResponse("Malformed JSON request."))
        case Left(_) => BadRequest(ErrorResponse("Malformed JSON request."))
      }

    case req @ POST -> Root / "ranking" / "result" =>
      req.attemptAs[RankingResultRequest].value.flatMap {
        case Right(request) => respond(service.recordResult(request))
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
