import play.sbt.PlayImport._
import sbt._, Keys._
import bloop.integrations.sbt.BloopKeys.bloopGenerate

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "4.0"
  val globalScalaVersion = "3.2.1"

  val shadedMongo = !System.getProperty("os.arch").toLowerCase.startsWith("aarch")
  if (shadedMongo) println("--- shaded native reactivemongo ---")

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version      := lilaVersion,
      organization := "org.lichess",
      resolvers ++= Seq(lilaMaven, sonashots),
      scalaVersion := globalScalaVersion,
      scalacOptions ++= compilerOptions,
      // No bloop project for tests
      Test / bloopGenerate := None,
      // disable publishing doc and sources
      Compile / doc / sources                := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      Compile / run / fork                   := true,
      javaOptions ++= Seq("-Xms64m", "-Xmx512m")
    )

  lazy val defaultLibs: Seq[ModuleID] =
    akka.bundle ++ macwire.bundle ++ Seq(
      cats,
      alleycats,
      play.api,
      chess,
      scalalib,
      jodaTime
    )

  def module(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
      libs: Seq[ModuleID]
  ) =
    Project(name, file("modules/" + name))
      .dependsOn(deps: _*)
      .settings(
        libraryDependencies ++= defaultLibs ++ libs,
        buildSettings,
        srcMain
      )

  val compilerOptions = Seq(
    // "-nowarn", // during migration
    // "-rewrite",
    // "-source:future-migration",
    // "-indent",
    // "-explaintypes",
    // "-explain",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-Xtarget:12"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource    := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
