import sbt.Keys._
import sbt._

object BuildSettings {

  import Dependencies._

  val lilaVersion        = "3.0"
  val globalScalaVersion = "2.13.16"

  val useEpoll = sys.props.get("epoll").fold(false)(_.toBoolean)
  if (useEpoll) println("--- epoll build ---")

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version      := lilaVersion,
      organization := "org.lishogi",
      scalaVersion := globalScalaVersion,
      resolvers ++= Dependencies.Resolvers.commons,
      scalacOptions ++= compilerOptions,
      Compile / doc / sources                := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
      Compile / run / fork                   := true,
      javaOptions ++= Seq("-Xms64m", "-Xmx256m"),
    )

  def baseLibs: Seq[ModuleID] = akka.bundle ++ macwire.bundle ++ reactivemongo.bundle ++ Seq(
    play.core,
    play.ws,
    play.json,
    play.jodaForms,
    play.specs2,
    scalatags,
    cats,
    alleycats,
    shogi,
    jodaTime,
    scaffeine,
    autoconfig,
    kamon.core,
  )

  lazy val moduleDepsMap = Map(
    "api"      -> Seq(hasher, kamon.influxdb),
    "common"   -> flexmark.bundle,
    "blog"     -> Seq(prismic),
    "db"       -> Seq(hasher, scrimage),
    "memo"     -> Seq(akka.testkit),
    "oauth"    -> Seq(galimatias, hasher),
    "push"     -> Seq(googleOAuth),
    "security" -> Seq(maxmind, hasher, uaparser),
    "socket"   -> Seq(lettuce),
    "user"     -> Seq(hasher),
  )

  def module(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
  ) =
    Project(
      name,
      file("modules/" + name),
    ).dependsOn(deps: _*)
      .settings(
        libraryDependencies ++= baseLibs ++ moduleDepsMap.getOrElse(name, Seq.empty),
        buildSettings,
        srcMain,
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
    "-unchecked",
    "-Xcheckinit",
    // Linting options
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
    // Warning options
    "-Wdead-code",
    "-Wextra-implicit",
    "-Wunused:imports",
    "-Wunused:patvars",
    "-Wunused:privates",
    "-Wunused:locals",
    "-Wunused:explicits",
    "-Wunused:implicits",
    "-Wmacros:after",
    "-Wvalue-discard",
    "-Werror",
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource    := (Test / sourceDirectory).value,
  )

}
