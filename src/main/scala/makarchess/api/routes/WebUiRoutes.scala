package makarchess.api.routes

import cats.effect.Async
import org.http4s.{HttpRoutes, Method, Request, StaticFile}
import org.http4s.dsl.Http4sDsl

import java.nio.file.{Files, Path => JPath, Paths}

final class WebUiRoutes[F[_]: Async] extends Http4sDsl[F]:
  private val webRoot = discoverWebRoot()
  private val indexPath = webRoot.resolve("index.html").normalize

  val routes: HttpRoutes[F] = HttpRoutes.of[F] {
    case req if req.method == Method.GET && isRootRequest(req) =>
      serveFile(req, indexPath)

    case req if req.method == Method.GET =>
      val relativePath = req.pathInfo.renderString.stripPrefix("/")
      val resolvedPath = webRoot.resolve(relativePath).normalize

      if !resolvedPath.startsWith(webRoot) || !Files.isRegularFile(resolvedPath) then NotFound()
      else serveFile(req, resolvedPath)
  }

  private def serveFile(req: Request[F], path: JPath) =
    StaticFile.fromFile(path.toFile, Some(req)).getOrElseF(NotFound())

  private def isRootRequest(req: Request[F]): Boolean =
    val path = req.pathInfo.renderString
    path.isBlank || path == "/"

  private def discoverWebRoot(): JPath =
    val current = Paths.get("").toAbsolutePath.normalize
    Iterator
      .iterate(Option(current))(_.flatMap(path => Option(path.getParent)))
      .takeWhile(_.nonEmpty)
      .flatten
      .map(_.resolve("web-ui").normalize)
      .find(path => Files.isRegularFile(path.resolve("index.html")))
      .getOrElse(current.resolve("web-ui").normalize)
