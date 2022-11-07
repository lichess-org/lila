import play.sbt.PlayImport._
import sbt._, Keys._
import bloop.integrations.sbt.BloopKeys.bloopGenerate

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "4.0"
  val globalScalaVersion = "3.2.0"

  val shadedMongo = !System.getProperty("os.arch").toLowerCase.startsWith("aarch")
  if (shadedMongo) println("--- shaded native reactivemongo ---")

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version      := lilaVersion,
      organization := "org.lichess",
      resolvers += lilaMaven,
      scalaVersion := globalScalaVersion,
      scalacOptions ++= compilerOptions,
      // No bloop project for tests
      Test / bloopGenerate := None,
      // disable publishing doc and sources
      Compile / doc / sources                := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      Compile / run / fork                   := true,
      javaOptions ++= Seq("-Xms64m", "-Xmx512m"),
      // com.typesafe.play:play-ahc-ws-standalone_2.13:2.1.3 brings in 0.9.0, but we want 1.0.0:
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"
    )

  lazy val defaultLibs: Seq[ModuleID] =
    akka.bundle ++ macwire.bundle ++ Seq(
      cats,
      alleycats,
      play.api,
      chess,
      scalalib,
      jodaTime,
      autoconfig
    )

  def smallModule(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
      libs: Seq[ModuleID]
  ) =
    Project(
      name,
      file("modules/" + name)
    ).dependsOn(deps: _*)
      .settings(
        libraryDependencies ++= libs,
        buildSettings,
        srcMain
      )

  def module(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
      libs: Seq[ModuleID]
  ) =
    smallModule(name, deps, defaultLibs ++ libs)

  val compilerOptions = Seq(
    "-encoding",
    "utf-8",
    "-rewrite",
    "-source:future-migration",
    "-indent",
    "-explaintypes",
    "-feature",
    "-language:postfixOps"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource    := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
