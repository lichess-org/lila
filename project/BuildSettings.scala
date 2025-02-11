import sbt.Keys._
import sbt._

object BuildSettings {

  import Dependencies._

  val globalLilaVersion  = "3.0"
  val globalOrganization = "org.lishogi"
  val globalScalaVersion = "2.13.16"

  def buildSettings =
    Defaults.coreDefaultSettings ++ Seq(
      version      := globalLilaVersion,
      organization := globalOrganization,
      scalaVersion := globalScalaVersion,
      resolvers ++= Dependencies.Resolvers.commons,
      scalacOptions ++= compilerOptions,
      Compile / doc / sources                := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false,
    )

  def baseLibs: Seq[ModuleID] = Seq(
    akka.actor,
    akka.typed,
    akka.stream,
    akka.slf4j,
    play.core,
    play.ws,
    play.json,
    play.specs2,
    reactivemongo.driver,
    reactivemongo.stream,
    cats.core,
    cats.alleycats,
    macwire.macros,
    macwire.util,
    jodaTime,
    apacheText,
    shogi,
    scaffeine,
    autoconfig,
  )

  def extraLibs(name: String) =
    name match {
      case "api" => Seq(hasher, galimatias)
      case "common" => {
        Seq(play.jodaForms, scalatags, kamon.core) ++ flexmark.bundle
      }
      case "db"       => Seq(hasher, scrimage)
      case "i18n"     => Seq(scalatags)
      case "memo"     => Seq(akka.testkit)
      case "oauth"    => Seq(galimatias, hasher)
      case "prismic"  => Seq(play.jsonJoda, galimatias)
      case "push"     => Seq(googleOAuth)
      case "security" => Seq(hasher, maxmind, uaparser, scalatags)
      case "socket"   => Seq(lettuce)
      case "user"     => Seq(hasher)
      case _          => Seq.empty
    }

  def rootLibs = baseLibs ++ Seq(scalatags, kamon.influxdb, kamon.metrics, kamon.prometheus)

  def module(
      name: String,
      deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]],
  ) =
    Project(
      name,
      file("modules/" + name),
    ).dependsOn(deps: _*)
      .settings(
        libraryDependencies ++= baseLibs ++ extraLibs(name),
        buildSettings,
        srcMain,
      )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource    := (Test / sourceDirectory).value,
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

}
