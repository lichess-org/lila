import sbt.*, Keys.*

object BuildSettings:

  import Dependencies.*

  val lilaVersion = "4.0"
  val globalScalaVersion = "3.10.0"

  def buildSettings =
    // NB: do NOT prepend Defaults.coreDefaultSettings here. Under sbt 2.0 those base settings are
    // already applied to every project automatically, and re-adding them forces the legacy flat
    // `target` layout (crossTarget = <proj>/target) instead of sbt 2.0's `target/out/...`. That
    // legacy layout places compile/package outputs outside the cache root, so sbt 2.0 floods the
    // log with "Cannot cache task because its output files are outside the output directory".
    Seq(
      resolvers ++= Seq(jitpack, lilaMaven, sonashots, Resolver.sonatypeCentralSnapshots),
      scalaVersion := globalScalaVersion,
      scalacOptions ++= Def.uncached(compilerOptions),
      javacOptions ++= Seq("--release", "21"),
      organization := "org.lichess",
      version := lilaVersion,
      Compile / doc / sources := Seq.empty,
      Compile / packageDoc / publishArtifact := false,
      Compile / packageSrc / publishArtifact := false
    )

  lazy val defaultLibs: Seq[ModuleID] =
    pekko.bundle ++ macwire.bundle ++ scalalib.bundle ++ Seq(
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
      .dependsOn(deps*)
      .settings(
        libraryDependencies ++= defaultLibs ++ libs,
        buildSettings,
        srcMain
      )

  val compilerOptions = Seq(
    // "-Ybackend-parallelism:16", // https://github.com/scala/scala3/pull/15392
    // "-explaintypes",
    // "-explain-cyclic",
    "-indent",
    "-feature",
    "-language:postfixOps",
    "-language:implicitConversions",
    "-release:21",
    "-Wimplausible-patterns",
    "-Wunused:all",
    "-Wunused:imports",
    "-Wconf:msg=method ArrowAssoc in object Predef is deprecated:silent",
    "-Wconf:msg=class ArrowAssoc is deprecated:silent"
    // "-Yprofile-enabled",
    // "-Yprofile-trace:compiler.trace"
  )

  val srcMain = Seq(
    Compile / scalaSource := (Compile / sourceDirectory).value,
    Test / scalaSource := (Test / sourceDirectory).value
  )

  def projectToRef(p: Project): ProjectReference = LocalProject(p.id)
