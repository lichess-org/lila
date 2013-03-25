import sbt._
import Keys._

object BuildSettings {

  import Dependencies._

  def buildSettings = Defaults.defaultSettings ++ Seq(
    organization := "org.lichess",
    scalaVersion := "2.10.1",
    resolvers ++= Dependencies.Resolvers.commons,
    parallelExecution in Test := false,
    scalacOptions := Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:_")
  )

  def defaultDeps = Seq(scalaz, scalalib, jodaTime, jodaConvert, scalaTime, spray.util)

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")
  def test(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "test")
  def runtime(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "runtime")
  def container(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "container")

  def project(name: String, deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =
    Project(
      name,
      file(name),
      dependencies = deps,
      settings = Seq(
        libraryDependencies := defaultDeps
      ) ++ buildSettings ++ srcMain
    )

  val srcMain = Seq(
    scalaSource in Compile <<= (sourceDirectory in Compile)(identity),
    scalaSource in Test <<= (sourceDirectory in Test)(identity)
  )
}
