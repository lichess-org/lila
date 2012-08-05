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
  val christophs = "Christophs Maven Repo" at "http://maven.henkelmann.eu/"
}

trait Dependencies {
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val specs2 = "org.specs2" %% "specs2" % "1.12"
  val salat = "com.novus" %% "salat-core" % "1.9.1-SNAPSHOT"
  val scalalib = "com.github.ornicar" %% "scalalib" % "1.37"
  val config = "com.typesafe" % "config" % "0.4.1"
  val guava = "com.google.guava" % "guava" % "13.0"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "org.scala-tools.time" %% "time" % "0.5"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val paginator = "com.github.ornicar" %% "paginator-core" % "1.6"
  val paginatorSalat = "com.github.ornicar" %% "paginator-salat-adapter" % "1.5"
  val csv = "com.github.tototoshi" %% "scala-csv" % "0.3"
  val hasher = "com.roundeights" % "hasher" % "0.3" from "http://cloud.github.com/downloads/Nycto/Hasher/hasher_2.9.1-0.3.jar"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
  val actuarius = "eu.henkelmann" %% "actuarius" % "0.2.3"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalastic = "default" % "scalastic_2.9.2" % "0.0.6-SNAPSHOT"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  private val buildSettings = Project.defaultSettings ++ Seq(
    organization := "com.github.ornicar",
    version := "1.2",
    scalaVersion := "2.9.1",
    resolvers := Seq(iliaz, codahale, sonatype, sonatypeS, typesafe, t2v, guice, jgitMaven, christophs),
    libraryDependencies := Seq(scalaz, scalalib, hasher),
    libraryDependencies in test := Seq(specs2),
    shellPrompt := {
      (state: State) ⇒ "%s> ".format(Project.extract(state).currentProject.id)
    },
    scalacOptions := Seq("-deprecation", "-unchecked")
  )

  lazy val lila = PlayProject("lila", mainLang = SCALA, settings = buildSettings).settings(
    libraryDependencies ++= Seq(
      config,
      salat,
      guava,
      apache,
      scalaTime,
      paginator,
      paginatorSalat,
      csv,
      jgit,
      actuarius,
      scalastic),
    templatesImport ++= Seq(
      "lila.game.{ DbGame, DbPlayer, Pov }",
      "lila.user.User",
      "lila.security.Permission",
      "lila.templating.Environment._",
      "lila.ui",
      "lila.http.Context",
      "com.github.ornicar.paginator.Paginator")
  ) dependsOn scalachess aggregate scalachess

  lazy val scalachess = Project("scalachess", file("scalachess")).settings(
    resolvers := Seq(iliaz),
    libraryDependencies := Seq(scalaz, scalalib, hasher, jodaTime, jodaConvert)
  )

  lazy val cli = Project("cli", file("cli"), settings = buildSettings).settings(
    libraryDependencies ++= Seq()
  ) dependsOn lila
}
