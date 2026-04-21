package makarchess.api

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import com.comcast.ip4s.{host, port}
import makarchess.api.routes.{AnalysisRoutes, BotRoutes, GameRoutes, RankingRoutes, WebUiRoutes}
import makarchess.api.service.{ApiAnalysisService, ApiBotService, ApiGameService, ApiRankingService, GameRegistry}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.CORS

object ServerApp extends IOApp.Simple:
  override def run: IO[Unit] =
    val registry = new GameRegistry()
    val gameService = new ApiGameService(registry)
    val botService = new ApiBotService(registry)
    val analysisService = new ApiAnalysisService(registry)
    val rankingService = new ApiRankingService()
    val routes =
      new GameRoutes[IO](gameService).routes <+>
        new BotRoutes[IO](botService).routes <+>
        new AnalysisRoutes[IO](analysisService).routes <+>
        new RankingRoutes[IO](rankingService).routes <+>
        new WebUiRoutes[IO]().routes
    val httpApp = Router(
      "/" -> routes
    ).orNotFound
    val corsApp = CORS.policy.withAllowOriginAll(httpApp)

    EmberServerBuilder
      .default[IO]
      .withHost(host"127.0.0.1")
      .withPort(port"8080")
      .withHttpApp(corsApp)
      .build
      .useForever
