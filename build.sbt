val scala3Version = "3.8.2"

lazy val osName = System.getProperty("os.name").toLowerCase
lazy val platform =
  if (osName.contains("win")) "win"
  else if (osName.contains("mac")) "mac"
  else "linux"

lazy val root = project
  .in(file("."))
  .settings(
    name := "MaKarChess",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    scalacOptions ++= Seq(
      "-Wconf:msg=Implicit parameters should be provided with a `using` clause:s"
    ),

    Compile / run / mainClass := Some("makarchess.Main"),

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.2.4" % Test,
      "org.typelevel" %% "munit-cats-effect-3" % "1.0.7" % Test,
      "org.typelevel" %% "cats-effect" % "3.5.7",
      "com.lihaoyi" %% "fastparse" % "3.1.1",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
      "com.lihaoyi" %% "upickle" % "3.3.1",
      "org.http4s" %% "http4s-ember-server" % "0.23.30",
      "org.http4s" %% "http4s-dsl" % "0.23.30",
      "org.http4s" %% "http4s-circe" % "0.23.30",
      "io.circe" %% "circe-core" % "0.14.10",
      "io.circe" %% "circe-generic" % "0.14.10",
      "io.circe" %% "circe-parser" % "0.14.10",
      "com.auth0" % "java-jwt" % "4.4.0",
      "com.auth0" % "jwks-rsa" % "0.22.1",
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "org.openjfx" % "javafx-base" % "21.0.2" classifier platform,
      "org.openjfx" % "javafx-controls" % "21.0.2" classifier platform,
      "org.openjfx" % "javafx-graphics" % "21.0.2" classifier platform
    )
  )
