import org.goldenport.cozy.CozyPlugin.autoImport._
import org.goldenport.cozy.CozyProjectIdentityEvidence
import sbt.Keys.*

lazy val projectIdentityEvidence = settingKey[CozyProjectIdentityEvidence]("Admitted project.yaml component identity evidence")
lazy val esa63LiveRuntimeClasspath = taskKey[File]("Export the compiled opt-in live harness runtime without executing it")
lazy val cncfDevelopmentRoot = sys.env.get("TEXTUS_KNOWLEDGE_LAKE_CNCF_SOURCE_ROOT").map { path =>
  val selected = file(path)
  require(selected.isAbsolute && selected.getCanonicalPath == path && (selected / "build.sbt").isFile,
    "Explicit canonical CNCF source build required")
  selected
}
lazy val cncfDevelopmentProject = cncfDevelopmentRoot.map(source => RootProject(source.toURI))
lazy val validateCncfDevelopment = settingKey[Unit]("Validate selected CNCF source coordinates")
lazy val cncfDevelopmentSettings: Seq[Def.Setting[_]] = cncfDevelopmentProject.toSeq.flatMap { source =>
  Seq(validateCncfDevelopment := {
    val expected = ProjectYamlBuild.dependencies(cozyProjectMetadata.value).find(_.name == "goldenport-cncf").get
    require((source / organization).value == expected.organization && (source / moduleName).value == expected.name &&
      (source / version).value == expected.revision && (source / scalaVersion).value == scalaVersion.value,
      "CNCF source coordinates must match the declared dependency")
  })
}

lazy val root = project
  .in(file("."))
  .dependsOn(cncfDevelopmentProject.toSeq.map(source => source % "compile->compile;runtime->runtime"): _*)
  .enablePlugins(org.goldenport.cozy.CozyPlugin)
  .settings(
    projectIdentityEvidence := ProjectYamlBuild.admitted(cozyProjectMetadata.value, scalaBinaryVersion.value),
    organization := ProjectYamlBuild.organization(projectIdentityEvidence.value, cozyProjectMetadata.value),
    moduleName := ProjectYamlBuild.moduleName(projectIdentityEvidence.value, cozyProjectMetadata.value),
    name := moduleName.value,
    version := ProjectYamlBuild.version(projectIdentityEvidence.value, cozyProjectMetadata.value),
    scalaVersion := ProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.scalaVersion"),
    useCoursier := false,
    Test / test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "org.scalatest.tags.Slow"),
    esa63LiveRuntimeClasspath := {
      val output = target.value / "esa63-live-runtime.txt"
      IO.write(output, (javaHome.value.getOrElse(file(sys.props("java.home"))) / "bin" / "java").getAbsolutePath + "\n" +
        (Test / fullClasspath).value.files.map(_.getAbsolutePath).mkString(java.io.File.pathSeparator) + "\n")
      output
    },
    resolvers += Resolver.defaultLocal,
    resolvers += Resolver.file("Local Ivy", file(Path.userHome.absolutePath + "/.ivy2/local"))(Resolver.ivyStylePatterns),
    resolvers += "Local Maven Repository" at ("file://" + Path.userHome.absolutePath + "/.m2/repository"),
    resolvers += "SimpleModeling.org" at "https://www.simplemodeling.org/repository/maven",
    validateCncfDevelopment := (),
    libraryDependencies ++= {
      validateCncfDevelopment.value
      ProjectYamlBuild.dependencies(cozyProjectMetadata.value).filterNot(module =>
        cncfDevelopmentRoot.isDefined && module.organization == "org.goldenport" && module.name == "goldenport-cncf")
    },

    cozyGeneratorBackend := "cozy",
    cozyDelegateProjectDir := None,
    cozyDelegateCommand := Seq(
      "cozy",
      "--runtime",
      ProjectYamlBuild.requiredValue(cozyProjectMetadata.value, "build.cozyVersion")
    ),
    cozyCarName := ProjectYamlBuild.carBaseName(projectIdentityEvidence.value, moduleName.value, version.value),
    cozyManifestMetadata ++= ProjectYamlBuild.manifestMetadata(projectIdentityEvidence.value, cozyProjectMetadata.value)
  )
  .settings(cncfDevelopmentSettings)
