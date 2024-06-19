ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / organization := "is.valsk"
ThisBuild / scalaVersion := "3.3.3"
ThisBuild / scalacOptions ++= Seq(
    "-Wunused:all",
    "-Wvalue-discard",
    "-unchecked",
    "-deprecation",
    "-feature",
)
ThisBuild / testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")

val zioVersion = "2.1.3"
val zioJsonVersion = "0.7.0"
val zioHttpVersion = "0.0.5"
val zioLoggingVersion = "2.3.0"
val zioTestVersion = "2.1.3"
val zioConfigVersion = "4.0.2"
val slf4jVersion = "2.0.13"
val refinedVersion = "0.11.2"
val quillVersion = "4.8.4"
val flywayVersion = "10.15.0"
val tapirVersion = "1.10.8"
val sttpVersion = "3.9.6"

val commonDependencies = Seq(
    "com.softwaremill.sttp.tapir" %% "tapir-sttp-client" % tapirVersion,
    "com.softwaremill.sttp.tapir" %% "tapir-json-zio" % tapirVersion,
    "com.softwaremill.sttp.client3" %% "zio" % sttpVersion
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
    "eu.timepit" %% "refined" % refinedVersion,
    "io.getquill" %% "quill-jdbc-zio" % quillVersion,
    "org.postgresql" % "postgresql" % "42.7.3",
    "org.flywaydb" % "flyway-core" % flywayVersion,
    "org.flywaydb" % "flyway-database-postgresql" % flywayVersion,
    "org.eclipse.angus" % "angus-mail" % "2.0.3",

    "dev.zio" %% "zio-test" % zioTestVersion % Test,
    "dev.zio" %% "zio-test-sbt" % zioTestVersion % Test,
    "dev.zio" %% "zio-test-magnolia" % zioTestVersion % Test,
    "dev.zio" %% "zio-mock" % "1.0.0-RC12",
)

lazy val common = crossProject(JVMPlatform, JSPlatform)
        .crossType(CrossType.Pure)
        .in(file("modules/common"))
        .settings(
            libraryDependencies ++= commonDependencies
        )
        .jsSettings(
            libraryDependencies ++= Seq(
                "io.github.cquiroz" %%% "scala-java-time" % "2.5.0" // implementations of java.time classes for Scala.JS,
            )
        )

lazy val app = (project in file("modules/app"))
        .settings(
            libraryDependencies ++= Seq(
                "com.softwaremill.sttp.tapir" %%% "tapir-sttp-client" % tapirVersion,
                "com.softwaremill.sttp.tapir" %%% "tapir-json-zio" % tapirVersion,
                "com.softwaremill.sttp.client3" %%% "zio" % sttpVersion,
                "dev.zio" %%% "zio-json" % "0.4.2",
                "io.frontroute" %%% "frontroute" % "0.18.1" // Brings in Laminar 16
            ),
            scalaJSLinkerConfig ~= {
                _.withModuleKind(ModuleKind.CommonJSModule)
            },
            semanticdbEnabled := true,
            autoAPIMappings := true,
            scalaJSUseMainModuleInitializer := true,
            Compile / mainClass := Some("is.valsk.esper.App")
        )
        .enablePlugins(ScalaJSPlugin)
        .dependsOn(common.js)

lazy val server = (project in file("modules/server"))
        .settings(
            libraryDependencies ++= serverDependencies
        )
        .dependsOn(common.jvm)

lazy val root = (project in file("."))
        .settings(
            name := "esper",
        )
        .aggregate(server, app)
        .dependsOn(server, app)

