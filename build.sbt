val scala3Version = "3.2.2"
val zioVersion = "2.0.10"
val zioLoggingVersion = "2.1.12"
val zioTestVersion = "2.0.13"
val zioConfigVersion = "3.0.7"
val circeVersion = "0.14.5"
val slf4jVersion = "2.0.5"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ESPer",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,

    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-nio" % "2.0.1",
      "dev.zio" %% "zio-json" % "0.4.2",
      "dev.zio" %% "zio-http" % "0.0.5",
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "eu.timepit" %% "refined" % "0.10.2",
      "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1",
      "org.postgresql" % "postgresql" % "42.5.4",

      "dev.zio" %% "zio-test" % zioTestVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioTestVersion % Test,
    ),

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
