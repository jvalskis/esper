val scala3Version = "3.3.3"

ThisBuild / organization := "is.valsk"
ThisBuild / version := "0.1.0-SNAPSHOT"
ThisBuild / scalaVersion := scala3Version

lazy val root = (project in file("."))
        .settings(
            name := "esper",
        )
        .aggregate(api)

lazy val api = (project in file("esper-api"))

