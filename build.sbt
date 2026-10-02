ThisBuild / scalaVersion := "3.3.8"
ThisBuild / organization := "org.simplemodeling.textus"
ThisBuild / version := "0.1.0-SNAPSHOT"

name := "textus-knowledge-lake"
externalResolvers := Seq(Resolver.mavenCentral)
libraryDependencies ++= Seq(
  "io.circe" %% "circe-core" % "0.14.6",
  "io.circe" %% "circe-generic" % "0.14.6",
  "io.circe" %% "circe-parser" % "0.14.6",
  "org.scalatest" %% "scalatest" % "3.2.19" % Test
)
