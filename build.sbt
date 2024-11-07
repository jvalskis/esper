import sbtrelease.ReleaseStateTransformations.*

import scala.sys.process.Process

ThisBuild / organization := "is.valsk"
ThisBuild / scalaVersion := "3.5.0"
ThisBuild / scalacOptions ++= Seq(
  "-Werror",
  "-Wunused:all",
  "-Wvalue-discard",
  "-unchecked",
  "-deprecation",
  "-feature",
)
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

val zioVersion = "2.1.11"
val zioJsonVersion = "0.7.0"
val zioHttpVersion = "3.0.1"
val zioLoggingVersion = "2.3.0"
val zioTestVersion = "2.1.3"
val zioConfigVersion = "4.0.2"
val slf4jVersion = "2.0.13"
val refinedVersion = "0.11.2"
val quillVersion = "4.8.4"
val flywayVersion = "10.15.0"
val tapirVersion = "1.10.9"
val sttpVersion = "3.9.7"

val commonDependencies = Seq(
  "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion,
  "com.softwaremill.sttp.client3" %% "zio" % sttpVersion,
  "eu.timepit" %% "refined" % refinedVersion,
)
val serverDependencies = commonDependencies ++ Seq(
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
  "io.getquill" %% "quill-jdbc-zio" % quillVersion,
  "org.postgresql" % "postgresql" % "42.7.3",
  "org.flywaydb" % "flyway-core" % flywayVersion,
  "org.flywaydb" % "flyway-database-postgresql" % flywayVersion,
  "org.eclipse.angus" % "angus-mail" % "2.0.3",
  "com.softwaremill.sttp.tapir" %% "tapir-zio" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-zio-http-server" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-swagger-ui-bundle" % tapirVersion,
  "com.softwaremill.sttp.tapir" %% "tapir-refined" % tapirVersion,

  "com.softwaremill.sttp.tapir" %% "tapir-sttp-stub-server" % tapirVersion % Test,
  "dev.zio" %% "zio-test-junit" % zioVersion % Test,
  "dev.zio" %% "zio-test" % zioTestVersion % Test,
  "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
  "dev.zio" %% "zio-test-magnolia" % zioTestVersion % Test,
  "dev.zio" %% "zio-mock" % "1.0.0-RC12" % Test,
)

lazy val common = crossProject(JVMPlatform, JSPlatform)
  .crossType(CrossType.Pure)
  .in(file("modules/common"))
  .settings(
    name := "esper-common",
    libraryDependencies ++= commonDependencies
  )
  .jsSettings(
    libraryDependencies ++= Seq(
      "io.github.cquiroz" %%% "scala-java-time" % "2.5.0" // implementations of java.time classes for Scala.JS,
    )
  )

val publishLocalImage = taskKey[Unit]("Packages all artifacts and creates a Docker image")
val buildJS = taskKey[Unit]("Build JS")

lazy val app = (project in file("modules/app"))
  .settings(
    libraryDependencies ++= Seq(
      "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % tapirVersion,
      "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % tapirVersion,
      "com.softwaremill.sttp.tapir" %%% "tapir-refined" % tapirVersion,
      "com.softwaremill.sttp.client3" %%% "zio" % sttpVersion,
      "dev.zio" %%% "zio-json" % zioJsonVersion,
      "eu.timepit" %% "refined" % refinedVersion,
      //                "dev.zio" %%% "zio-config" % zioConfigVersion,// TODO enable when zio-config is available in scala3 + scalajs
      //                "dev.zio" %%% "zio-config-magnolia" % zioConfigVersion,
      "io.frontroute" %%% "frontroute" % "0.18.1" // Brings in Laminar 16
    ),
    scalaJSLinkerConfig ~= {
      _.withModuleKind(ModuleKind.CommonJSModule)
    },
    semanticdbEnabled := true,
    autoAPIMappings := true,
    scalaJSUseMainModuleInitializer := true,
    Compile / mainClass := Some("is.valsk.esper.App"),
    // Normalize output of fastOptJS and fullOptJS
    Seq(fastOptJS, fullOptJS).map(task =>
      Compile / task / artifactPath := ((Compile / task / crossTarget).value / "main.js"),
    ),
    cleanFiles ++= Seq(
      baseDirectory.value / "dist",
    ),

    stage := {
      // Generate Scala.js JS output for production
      (Compile / fullOptJS).value

      // Install JS dependencies from package-lock.json
      val npmCiExitCode = Process("npm ci --include=dev", cwd = baseDirectory.value).!
      if (npmCiExitCode > 0) {
        throw new IllegalStateException(s"npm ci failed. See above for reason")
      }

      // Build the frontend with vite
      val buildExitCode = Process("npm run build-prod", cwd = baseDirectory.value).!
      if (buildExitCode > 0) {
        throw new IllegalStateException(s"Building frontend failed. See above for reason")
      }
      baseDirectory.value / "target" / "staging"
    },
  )
  .enablePlugins(ScalaJSPlugin)
  .dependsOn(common.js)

lazy val stagingApp = (project in file("build/staging-app"))
  .enablePlugins(JavaAppPackaging)
  .settings(
    name := "esper-app",
    Compile / unmanagedResourceDirectories += (app / baseDirectory).value / "target" / "staging",
    publishLocal := publishLocal.dependsOn(app / stage).value,
  )

lazy val server = (project in file("modules/server"))
  .settings(
    name := "esper-server",
    libraryDependencies ++= serverDependencies,
  )
  .dependsOn(common.jvm)

lazy val root = (project in file("."))
  .settings(
    name := "esper",
    Compile / doc / sources := Seq.empty,
    Compile / packageDoc / publishArtifact := false,
    publish / skip := true,
    releaseProcess := Seq[ReleaseStep](
      checkSnapshotDependencies,
      inquireVersions,
      runClean,
      runTest,
      setReleaseVersion,
      commitReleaseVersion,
      releaseStepTask(stagingBuild / Docker / publish),
      setNextVersion,
      commitNextVersion,
      pushChanges
    )
  )
  .aggregate(server, app, stagingBuild, stagingApp)
  .dependsOn(server, app)

lazy val stagingBuild = (project in file("build/staging"))
  .enablePlugins(JavaAppPackaging, DockerPlugin)
  .settings(
    name := "esper",
    dockerBaseImage := "openjdk:21-slim-buster",
    dockerExposedPorts ++= Seq(9000),
    dockerUsername := sys.env.get("DOCKER_USERNAME"),
    dockerRepository := sys.env.get("DOCKER_REPOSITORY"),
    dockerUpdateLatest := true,
    Compile / mainClass := Some("is.valsk.esper.Application"),
    Docker / publishLocal := (Docker / publishLocal).dependsOn(
      stagingApp / publishLocal
    ).value,
    Docker / publish := (Docker / publish).dependsOn(
      stagingApp / publishLocal
    ).value
  )
  .dependsOn(server, stagingApp)