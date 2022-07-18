import play.sbt.PlayImport._
import sbt._, Keys._

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "3.0"
  val globalScalaVersion = "2.13.8"

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
      // com.typesafe.play:play-ahc-ws-standalone_2.13:2.1.3 brings in 0.9.0, but we want 1.0.0:
      libraryDependencySchemes += "org.scala-lang.modules" %% "scala-java8-compat" % "always"
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
    //"-Xfatal-warnings",
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
    //"-Wunused:locals",
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
