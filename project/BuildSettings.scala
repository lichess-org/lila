import play.sbt.PlayImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val lilaVersion = "4.0"
  val globalScalaVersion = "3.7.2"

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      resolvers ++= Seq(jitpack, lilaMaven, sonashots, Resolver.sonatypeCentralSnapshots),
      scalaVersion := globalScalaVersion,
      scalacOptions ++= compilerOptions,
      javacOptions ++= Seq("--release", "21"),
      organization := "org.lichess",
      version := lilaVersion,
      Compile / doc / sources := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )

  lazy val defaultLibs: Seq[ModuleID] =
    akka.bundle ++ macwire.bundle ++ scalalib.bundle ++ Seq(
      cats,
      alleycats,
      play.api,
      chess.core,
      chess.rating,
      kittens
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
    "-Ybackend-parallelism:16", // https://github.com/scala/scala3/pull/15392
    // "-nowarn", // during migration
    // "-rewrite",
    // "-source:3.7-migration",
    "-indent",
    // "-explaintypes",
    // "-explain-cyclic",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-release:21",
    "-Wimplausible-patterns",
    "-Wunused:all"
    // "-Yprofile-enabled",
    // "-Yprofile-trace:compiler.trace"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
