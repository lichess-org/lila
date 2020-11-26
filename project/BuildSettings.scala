import play.sbt.PlayImport._
import sbt._, Keys._
import bloop.integrations.sbt.BloopKeys.bloopGenerate

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "3.2"
  val globalScalaVersion = "2.13.4"

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
      bloopGenerate in Test := None,
      // disable publishing doc and sources
      sources in (Compile, doc) := Seq.empty,
      publishArtifact in (Compile, packageDoc) := false,
      publishArtifact in (Compile, packageSrc) := false
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
    // "-Xfatal-warnings",
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
    "-Wunused:imports",
    "-Wunused:locals",
    "-Wunused:patvars",
    // "-Wunused:privates", // unfortunately doesn't work with macros
    // "-Wunused:implicits",
    // "-Wunused:params"
    "-Wvalue-discard"
  )

  val srcMain = Seq(
    scalaSource in Compile := (sourceDirectory in Compile).value,
    scalaSource in Test := (sourceDirectory in Test).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
