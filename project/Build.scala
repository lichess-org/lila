import sbt._
import Keys._
import PlayProject._

trait Resolvers {
  val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
  val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
  val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
  val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
  val sonatypeS = "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  val guice = "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk"
  val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
}

trait Dependencies {
  val scalachess = "com.github.ornicar" %% "scalachess" % "1.14"
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val specs2 = "org.specs2" %% "specs2" % "1.11"
  val casbah = "org.mongodb" %% "casbah" % "2.3.0"
  val salat = "com.novus" %% "salat-core" % "1.9-SNAPSHOT"
  val scalalib = "com.github.ornicar" %% "scalalib" % "1.37"
  val config = "com.typesafe" % "config" % "0.4.0"
  val json = "com.codahale" %% "jerkson" % "0.5.0"
  val guava = "com.google.guava" % "guava" % "12.0"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "org.scala-tools.time" %% "time" % "0.5"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val dispatch = "net.databinder" %% "dispatch-http" % "0.8.7"
  val paginator = "com.github.ornicar" %% "paginator-core" % "1.5"
  val paginatorSalat = "com.github.ornicar" %% "paginator-salat-adapter" % "1.4"
  val csv = "com.github.tototoshi" %% "scala-csv" % "0.3"
  val hasher = "com.roundeights" % "hasher" % "0.3" from "http://cloud.github.com/downloads/Nycto/Hasher/hasher_2.9.1-0.3.jar"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  private val buildSettings = Project.defaultSettings ++ Seq(
    organization := "com.github.ornicar",
    version := "1.0",
    scalaVersion := "2.9.1",
    resolvers := Seq(iliaz, codahale, sonatype, sonatypeS, typesafe, t2v, guice, jgitMaven),
    libraryDependencies := Seq(scalaz, scalalib, hasher),
    libraryDependencies in test := Seq(specs2),
    shellPrompt := {
      (state: State) ⇒ "%s> ".format(Project.extract(state).currentProject.id)
    },
    scalacOptions := Seq("-deprecation", "-unchecked")
  )

  lazy val lila = PlayProject("lila", mainLang = SCALA, settings = buildSettings).settings(
    libraryDependencies ++= Seq(
      scalachess,
      config,
      json,
      casbah,
      salat,
      guava,
      apache,
      scalaTime,
      dispatch,
      paginator,
      paginatorSalat,
      csv,
      jgit),
    templatesImport ++= Seq(
      "lila.game.{ DbGame, DbPlayer, Pov }",
      "lila.user.User",
      "lila.security.Permission",
      "lila.templating.Environment._",
      "lila.ui",
      "lila.http.Context",
      "com.github.ornicar.paginator.Paginator")
    //incrementalAssetsCompilation := true,
    //javascriptEntryPoints <<= (sourceDirectory in Compile)(base ⇒
      //((base / "assets" / "javascripts" ** "*.js") 
        //--- (base / "assets" / "javascripts" ** "_*")
        //--- (base / "assets" / "javascripts" / "vendor" ** "*.js")
        //--- (base / "assets" / "javascripts" ** "*.min.js")
      //).get
    //),
    //lessEntryPoints <<= baseDirectory(_ / "app" / "assets" / "stylesheets" ** "*.less")
  ) 

  lazy val cli = Project("cli", file("cli"), settings = buildSettings).settings(
    libraryDependencies ++= Seq()
  ) dependsOn (lila)
}
