ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / scalacOptions ++= Seq(
    "-Wunused:all",
    "-Wvalue-discard",
    "-unchecked",
    "-deprecation",
    "-feature",
)
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
ThisBuild / Compile / run / mainClass := Some("is.valsk.esper.Main")
ThisBuild / Docker / packageName := "justasv/esper-api"
ThisBuild / Docker / dockerExposedPorts := Seq(9000)
ThisBuild / assembly / mainClass := Some("is.valsk.esper.Main")
ThisBuild / assembly / assemblyMergeStrategy := {
    case PathList("META-INF", "services", _*) => MergeStrategy.deduplicate
    case PathList("META-INF", _*) => MergeStrategy.discard
    case _ => MergeStrategy.first
}
ThisBuild / assembly / assemblyJarName := s"${name.value}-${version.value}.jar"

val zioVersion = "2.1.3"
val zioJsonVersion = "0.7.0"
val zioHttpVersion = "0.0.5"
val zioLoggingVersion = "2.3.0"
val zioTestVersion = "2.1.3"
val zioConfigVersion = "4.0.2"
val slf4jVersion = "2.0.5"
val refinedVersion = "0.11.2"
val quillVersion = "4.8.4"
val flywayVersion = "10.15.0"

val dependencies = Seq(
    "org.slf4j" % "slf4j-simple" % slf4jVersion,
    "dev.zio" %% "zio-logging" % zioLoggingVersion,
    "dev.zio" %% "zio-logging-slf4j" % zioLoggingVersion,
    "dev.zio" %% "zio" % zioVersion,
    "dev.zio" %% "zio-streams" % zioVersion,
    "dev.zio" %% "zio-json" % zioJsonVersion,
    "dev.zio" %% "zio-http" % zioHttpVersion,
    "dev.zio" %% "zio-config" % zioConfigVersion,
    "dev.zio" %% "zio-config-typesafe" % zioConfigVersion,
    "dev.zio" %% "zio-config-magnolia" % zioConfigVersion,
    "eu.timepit" %% "refined" % refinedVersion,
    "io.getquill" %% "quill-jdbc-zio" % quillVersion,
    "org.postgresql" % "postgresql" % "42.5.4",
    "org.flywaydb" % "flyway-core" % flywayVersion,
    "org.flywaydb" % "flyway-database-postgresql" % flywayVersion,

    "dev.zio" %% "zio-test" % zioTestVersion % Test,
    "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
    "dev.zio" %% "zio-test-magnolia" % zioTestVersion % Test,
)

enablePlugins(JavaAppPackaging)

lazy val root = (project in file("."))
    .settings(
        name := "esper-api",
        libraryDependencies ++= dependencies,
    )
