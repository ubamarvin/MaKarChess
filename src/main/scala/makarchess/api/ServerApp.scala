package makarchess.api

import cats.effect.{IO, IOApp}
import com.comcast.ip4s.{host, port}
import makarchess.api.routes.GameRoutes
import makarchess.api.service.{ApiGameService, GameRegistry}
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.server.Router

object ServerApp extends IOApp.Simple:
  override def run: IO[Unit] =
    val registry = new GameRegistry()
    val service = new ApiGameService(registry)
    val httpApp = Router(
      "/" -> new GameRoutes[IO](service).routes
    ).orNotFound

    EmberServerBuilder
      .default[IO]
      .withHost(host"127.0.0.1")
      .withPort(port"8080")
      .withHttpApp(httpApp)
      .build
      .useForever
