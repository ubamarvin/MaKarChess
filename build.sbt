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

    Compile / run / mainClass := Some("makarchess.Main"),

    libraryDependencies ++= Seq(
      "org.scalameta" %% "munit" % "1.2.4" % Test,
      "org.scalafx" %% "scalafx" % "21.0.0-R32",
      "org.openjfx" % "javafx-base" % "21.0.2" classifier platform,
      "org.openjfx" % "javafx-controls" % "21.0.2" classifier platform,
      "org.openjfx" % "javafx-graphics" % "21.0.2" classifier platform
    )
  )
