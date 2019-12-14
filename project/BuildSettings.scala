import play.sbt.PlayImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val globalScalaVersion = "2.13.1"

  def buildSettings = Defaults.coreDefaultSettings ++ Seq(
    version := "3.0",
    organization := "org.lichess",
    scalaVersion := globalScalaVersion,
    resolvers ++= Dependencies.Resolvers.commons,
    scalacOptions ++= compilerOptions,
    sources in doc in Compile := List(),
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false
  )

  def defaultLibs: Seq[ModuleID] = Seq(
    play.api,
    scalaz,
    chess,
    scalalib,
    jodaTime,
    ws,
    macwire.macros,
    macwire.util,
    autoconfig,
    specs2
  )

  def module(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
      libs: Seq[ModuleID]
  ) =
    Project(
      name,
      file("modules/" + name)
    ).dependsOn(deps: _*)
      .settings(
        libraryDependencies ++= defaultLibs ++ libs ++ silencer.bundle,
        buildSettings,
        srcMain
      )

  val compilerOptions = Seq(
    "-language:implicitConversions",
    "-language:postfixOps",
    "-feature",
    "-unchecked",
    "-deprecation",
    "-Xlint:_",
    "-Ywarn-macros:after",
    "-Ywarn-unused:_",
    // "-Xfatal-warnings",
    "-Xmaxerrs",
    "15",
    "-Xmaxwarns",
    "12"
  )

  val srcMain = Seq(
    scalaSource in Compile := (sourceDirectory in Compile).value,
    scalaSource in Test := (sourceDirectory in Test).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
