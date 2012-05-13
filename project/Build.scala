import sbt._
import Keys._
import PlayProject._

trait Resolvers {
  val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
  val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
  val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
  val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
  val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  val guice = "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk"
}

trait Dependencies {
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val specs2 = "org.specs2" %% "specs2" % "1.8.2"
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
  val salat = "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT"
  val scalalib = "com.github.ornicar" %% "scalalib" % "1.30"
  val hasher = "com.roundeights" % "hasher" % "0.3" from "http://cloud.github.com/downloads/Nycto/Hasher/hasher_2.9.1-0.3.jar"
  val config = "com.typesafe" % "config" % "0.4.0"
  val json = "com.codahale" %% "jerkson" % "0.5.0"
  val guava = "com.google.guava" % "guava" % "11.0.2"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalaTime = "org.scala-tools.time" %% "time" % "0.5"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.7"
  val auth = "jp.t2v" %% "play20.auth" % "0.3-SNAPSHOT"
  val plugins = "com.typesafe" %% "play-plugins-redis" % "2.0.1-hack2"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  private val buildSettings = Project.defaultSettings ++ Seq(
    organization := "com.github.ornicar",
    version := "0.1",
    scalaVersion := "2.9.1",
    resolvers := Seq(iliaz, codahale, sonatype, typesafe, t2v, guice),
    libraryDependencies := Seq(scalaz, scalalib),
    libraryDependencies in test := Seq(specs2),
    shellPrompt := {
      (state: State) ⇒ "%s> ".format(Project.extract(state).currentProject.id)
    },
    scalacOptions := Seq("-deprecation", "-unchecked")
  )

  lazy val lila = PlayProject("lila", mainLang = SCALA, settings = buildSettings).settings(
    libraryDependencies ++= Seq(
      config,
      json,
      casbah,
      salat,
      guava,
      apache,
      jodaTime,
      jodaConvert,
      scalaTime,
      dispatch,
      auth,
      plugins),
    templatesImport ++= Seq(
      "lila.model._",
      "lila.templating.Environment._",
      "lila.ui.SiteMenu",
      "lila.http.Context"),
    incrementalAssetsCompilation := true,
    javascriptEntryPoints <<= (sourceDirectory in Compile)(base ⇒
      ((base / "assets" / "javascripts" ** "*.js") 
        --- (base / "assets" / "javascripts" ** "_*")
        --- (base / "assets" / "javascripts" / "vendor" ** "*.js")
        --- (base / "assets" / "javascripts" ** "*.min.js")
      ).get
    ),
    lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" ** "*.less")
  ) dependsOn chess

  lazy val cli = Project("cli", file("cli"), settings = buildSettings).settings(
    libraryDependencies ++= Seq()
  ) dependsOn (lila)

  lazy val chess = Project("chess", file("chess"), settings = buildSettings).settings(
    libraryDependencies ++= Seq(hasher)
  )
}
