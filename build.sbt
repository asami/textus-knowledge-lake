import org.goldenport.cozy.CozyPlugin.autoImport._

ThisBuild / scalaVersion := "3.3.8"
ThisBuild / organization := "org.simplemodeling.textus"
ThisBuild / version := "0.1.0-SNAPSHOT"

lazy val root = (project in file("."))
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .settings(
    name := "textus-knowledge-lake",
    externalResolvers := Seq(
      Resolver.mavenCentral,
      "SimpleModeling Maven" at "https://www.simplemodeling.org/repository/maven"
    ),
    libraryDependencies ++= Seq(
      "org.goldenport" %% "goldenport-cncf" % "0.5.2",
      "io.circe" %% "circe-core" % "0.14.6",
      "io.circe" %% "circe-generic" % "0.14.6",
      "io.circe" %% "circe-parser" % "0.14.6",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    ),
    cozyDelegateProjectDir := None,
    cozyDelegateCoursierVersion := Some("0.3.2.2"),
    cozySkipUnchangedGeneration := false,
    cozyDelegateCommand := Seq(
      "cozy", "--launcher-config",
      (baseDirectory.value / "conf" / "cozy" / "launcher.yaml").getAbsolutePath
    )
  )
