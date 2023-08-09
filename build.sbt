val scala3Version = "3.3.0"
val zioVersion = "2.0.13"
val zioNioVersion = "2.0.1"
val zioJsonVersion = "0.5.0"
val zioHttpVersion = "0.0.5"
val zioLoggingVersion = "2.1.12"
val zioTestVersion = "2.0.13"
val zioConfigVersion = "3.0.7"
val circeVersion = "0.14.5"
val slf4jVersion = "2.0.5"
val refinedVersion = "0.10.3"

lazy val root = project
  .in(file("."))
  .settings(
    name := "ESPer",
    version := "0.1.0-SNAPSHOT",

    scalaVersion := scala3Version,
    scalacOptions ++= Seq(
      "-Wunused:all",
      "-Wvalue-discard"
    ),

    libraryDependencies ++= Seq(
      "org.slf4j" % "slf4j-simple" % slf4jVersion,
      "dev.zio" %% "zio-logging" % zioLoggingVersion,
      "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
      "dev.zio" %% "zio" % zioVersion,
      "dev.zio" %% "zio-streams" % zioVersion,
      "dev.zio" %% "zio-nio" % zioNioVersion,
      "dev.zio" %% "zio-json" % zioJsonVersion,
      "dev.zio" %% "zio-http" % zioHttpVersion,
      "dev.zio" %% "zio-config" % zioConfigVersion,
      "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
      "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
      "eu.timepit" %% "refined" % refinedVersion,
      "io.getquill" %% "quill-jdbc-zio" % "4.6.0.1",
      "org.postgresql" % "postgresql" % "42.5.4",

      "dev.zio" %% "zio-test" % zioTestVersion % Test,
      "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
      "dev.zio" %% "zio-test-magnolia" % zioTestVersion % Test,
    ),

    assembly / mainClass := Some("is.valsk.esper.Main"),
    assembly / assemblyMergeStrategy := {
      case PathList("META-INF", "services", xs@_*) => MergeStrategy.deduplicate
      case PathList("META-INF", xs@_*) => MergeStrategy.discard
      case _ => MergeStrategy.first
    },
    assembly / assemblyJarName := s"${name.value}-${version.value}.jar",

    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  )
