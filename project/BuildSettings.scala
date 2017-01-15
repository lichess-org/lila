import play.sbt.Play.autoImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val globalScalaVersion = "2.11.8"

  def buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "org.lichess",
    scalaVersion := globalScalaVersion,
    resolvers ++= Dependencies.Resolvers.commons,
    parallelExecution in Test := false,
    scalacOptions := compilerOptions,
    incOptions := incOptions.value.withNameHashing(true),
    updateOptions := updateOptions.value.withCachedResolution(true),
    sources in doc in Compile := List(),
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false
  )

  def defaultDeps = Seq(scalaz, scalalib, jodaTime, spray.util, ws, java8compat)

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")

  def project(name: String, deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =
    Project(
      name,
      file("modules/" + name),
      dependencies = deps,
      settings = Seq(
        version := "2.0",
        libraryDependencies := defaultDeps
      ) ++ buildSettings ++ srcMain
    )

  val compilerOptions = Seq(
    "-deprecation", "-unchecked", "-feature", "-language:_",
    // "-Xfatal-warnings",
    // "-Ywarn-unused-import", 
    "-Ywarn-dead-code",
    "-Ywarn-unused",
    // "-Xlint:missing-interpolator",
    "-Ybackend:GenBCode", "-Ydelambdafy:method", "-target:jvm-1.8")

  val srcMain = Seq(
    scalaSource in Compile := (sourceDirectory in Compile).value,
    scalaSource in Test := (sourceDirectory in Test).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
