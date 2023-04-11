val scala3Version = "3.2.2"
val zioVersion = "2.0.10"
val zioConfigVersion = "3.0.7"
val circeVersion = "0.14.5"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ESPer",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-test" % zioVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioVersion % Test,
      "dev.zio" %% "zio-nio" % "2.0.1",
      "dev.zio" %% "zio-json" % "0.4.2",
      "dev.zio" %% "zio-http" % "0.0.5",
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "eu.timepit" %% "refined" % "0.10.2",
    ),

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
