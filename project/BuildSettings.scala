import com.typesafe.sbt.SbtScalariform.autoImport.scalariformPreferences
import play.sbt.PlayImport._
import sbt._, Keys._
import scalariform.formatter.preferences._

object BuildSettings {

  import Dependencies._

  val globalScalaVersion = "2.13.1"

  def buildSettings = Defaults.coreDefaultSettings ++ Seq(
    organization := "org.lichess",
    scalaVersion := globalScalaVersion,
    resolvers ++= Dependencies.Resolvers.commons,
    scalacOptions ++= compilerOptions,
    sources in doc in Compile := List(),
    // disable publishing the main API jar
    publishArtifact in (Compile, packageDoc) := false,
    // disable publishing the main sources jar
    publishArtifact in (Compile, packageSrc) := false,
    scalariformPreferences := scalariformPrefs(scalariformPreferences.value)
  )

  def scalariformPrefs(prefs: IFormattingPreferences) = prefs
    .setPreference(DanglingCloseParenthesis, Force)
    .setPreference(DoubleIndentConstructorArguments, true)

  def defaultDeps = Seq(
    scalaz, chess, scalalib, jodaTime, ws,
    macwire.macros, macwire.util, autoconfig
  ) // , specs2, specs2Scalaz)

  def compile(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "compile")
  def provided(deps: ModuleID*): Seq[ModuleID] = deps map (_ % "provided")

  def module(name: String, deps: Seq[sbt.ClasspathDep[sbt.ProjectReference]] = Seq.empty) =
    Project(
      name,
      file("modules/" + name)
    )
      .dependsOn(deps: _*)
      .settings(
        version := "2.0",
        libraryDependencies ++= defaultDeps,
        buildSettings,
        srcMain
      )

  val compilerOptions = Seq(
    "-language:implicitConversions",
    "-language:postfixOps",
    "-language:reflectiveCalls", // #TODO remove me for perfs
    "-feature",
    "-deprecation",
    "-unchecked",
    "-Wunused:imports,locals,implicits",
    "inaccessible,infer-any",
    "-Xmaxerrs", "5",
    "-Xmaxwarns", "5"
  )

  val srcMain = Seq(
    scalaSource in Compile := (sourceDirectory in Compile).value,
    scalaSource in Test := (sourceDirectory in Test).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
}
