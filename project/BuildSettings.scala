import play.Play.autoImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val globalScalaVersion = "2.11.7"

  def buildSettings = Defaults.defaultSettings ++ Seq(
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

  def defaultDeps = Seq(scalaz, scalalib, jodaTime, spray.util, ws, kamon.core)

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

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

  val compilerOptions = Seq("-deprecation", "-unchecked", "-feature", "-language:_")

  val srcMain = Seq(
    scalaSource in Compile <<= (sourceDirectory in Compile)(identity),
    scalaSource in Test <<= (sourceDirectory in Test)(identity)
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
