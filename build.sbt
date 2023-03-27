val scala3Version = "3.2.2"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ESPer",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % "2.0.10",
      "dev.zio" %% "zio-streams" % "2.0.10",
      "dev.zio" %% "zio-json" % "0.4.2",
      "dev.zio" %% "zio-http" % "0.0.5",
      "org.scalameta" %% "munit" % "0.7.29" % Test
    )
  )
