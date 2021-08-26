import play.sbt.PlayImport._
import sbt._, Keys._
import bloop.integrations.sbt.BloopKeys.bloopGenerate

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "3.2"
  val globalScalaVersion = "2.13.6"

  val useEpoll = sys.props.get("epoll").fold(false)(_.toBoolean)
  if (useEpoll) println("--- epoll build ---")

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version := lilaVersion,
      organization := "org.lichess",
      resolvers += lilaMaven,
      scalaVersion := globalScalaVersion,
      scalacOptions ++= compilerOptions,
      // No bloop project for tests
      Test / bloopGenerate := None,
      // disable publishing doc and sources
      Compile / doc / sources := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      javaOptions ++= Seq("-Xms64m", "-Xmx256m"),
      // com.typesafe.play:play-ahc-ws-standalone_2.13:2.1.3 brings in 0.9.0, but we want 1.0.0:
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"
    )

  lazy val defaultLibs: Seq[ModuleID] =
    akka.bundle ++ macwire.bundle ++ Seq(
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
    "-explaintypes",
    "-feature",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-language:postfixOps",
    "-Ymacro-annotations",
    // Warnings as errors!
    "-Xfatal-warnings",
    // Linting options
    "-unchecked",
    "-Xcheckinit",
    "-Xlint:adapted-args",
    "-Xlint:constant",
    "-Xlint:delayedinit-select",
    "-Xlint:deprecation",
    "-Xlint:inaccessible",
    "-Xlint:infer-any",
    "-Xlint:missing-interpolator",
    "-Xlint:nullary-unit",
    "-Xlint:option-implicit",
    "-Xlint:package-object-classes",
    "-Xlint:poly-implicit-overload",
    "-Xlint:private-shadow",
    "-Xlint:stars-align",
    "-Xlint:type-parameter-shadow",
    "-Wdead-code",
    "-Wextra-implicit",
    // "-Wnumeric-widen",
    // "-Wunused:imports",
    // "-Wunused:locals",
    "-Wunused:patvars",
    // "-Wunused:privates",  // unfortunately doesn't work with wire macros
    // "-Wunused:implicits", // unfortunately doesn't work with wire macros
    // "-Wunused:params"     // unfortunately doesn't work with wire macros
    "-Wvalue-discard"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
