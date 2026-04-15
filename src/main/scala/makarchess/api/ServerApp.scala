package makarchess.api

import cats.effect.{IO, IOApp}
import cats.syntax.all.*
import com.comcast.ip4s.{host, port}
import makarchess.api.routes.{GameRoutes, WebUiRoutes}
import makarchess.api.service.{ApiGameService, GameRegistry}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router
import org.http4s.server.middleware.CORS

object ServerApp extends IOApp.Simple:
  override def run: IO[Unit] =
    val registry = new GameRegistry()
    val service = new ApiGameService(registry)
    val routes = new GameRoutes[IO](service).routes <+> new WebUiRoutes[IO]().routes
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
