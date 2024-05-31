import play.sbt.PlayImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "3.0"
  val globalScalaVersion = "2.13.14"

  val useEpoll = sys.props.get("epoll").fold(false)(_.toBoolean)
  if (useEpoll) println("--- epoll build ---")

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version := lilaVersion,
      organization := "org.lishogi",
      scalaVersion := globalScalaVersion,
      resolvers ++= Dependencies.Resolvers.commons,
      scalacOptions ++= compilerOptions,
      // disable publishing doc and sources
      Compile / doc / sources := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      Compile / run / fork                   := true,
      javaOptions ++= Seq("-Xms64m", "-Xmx256m"),
    )

  def defaultLibs: Seq[ModuleID] =
    akka.bundle ++ Seq(
      play.api,
      scalalib,
      shogi,
      jodaTime,
      ws,
      macwire.macros,
      macwire.util,
      autoconfig
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
        libraryDependencies ++= defaultLibs ++ libs,
        buildSettings,
        srcMain
      )

  val compilerOptions = Seq(
    "-encoding",
    "utf-8",
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
    "-Wconf:cat=other-implicit-type:s",
    "-Wdead-code",
    "-Wextra-implicit",
    // "-Wnumeric-widen",
    "-Wunused:imports",
    "-Wunused:locals",
    "-Wunused:patvars",
    "-Wunused:privates",
    "-Wunused:implicits",
    "-Wunused:explicits",
    "-Wmacros:after",
    "-Wvalue-discard"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
