import sbt._
import Keys._
import play.Project._

trait Resolvers {
  // val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
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
  val scalaz = "org.scalaz" % "scalaz-core_2.10.0-RC3" % "6.0.4"
  val salat = "com.novus" % "salat-core_2.9.2" % "1.9.1"
  val scalalib = "com.github.ornicar" % "scalalib_2.9.1" % "2.5"
  val config = "com.typesafe" % "config" % "1.0.0"
  val guava = "com.google.guava" % "guava" % "13.0.1"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "org.scala-tools.time" % "time_2.9.1" % "0.5"
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val paginator = "com.github.ornicar" % "paginator-core_2.9.1" % "1.6"
  val paginatorSalat = "com.github.ornicar" % "paginator-salat-adapter_2.9.1" % "1.5"
  val csv = "com.github.tototoshi" % "scala-csv_2.9.1" % "0.3"
  val hasher = "com.roundeights" % "hasher" % "0.3" from "http://cloud.github.com/downloads/Nycto/Hasher/hasher_2.9.1-0.3.jar"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
  val actuarius = "eu.henkelmann" % "actuarius_2.9.2" % "0.2.4"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalastic = "com.traackr" % "scalastic_2.9.2" % "0.0.6-HACKED"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "1.3.+"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  // private val buildSettings = Project.defaultSettings ++ Seq(
  private val buildSettings = Seq(
    shellPrompt := {
      (state: State) ⇒ "%s> ".format(Project.extract(state).currentProject.id)
    },
    scalacOptions := Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:_")
  )

  lazy val lila = play.Project("lila", "3", Seq(
    scalaz, scalalib, hasher, config, salat, guava, apache, scalaTime,
    paginator, paginatorSalat, csv, jgit, actuarius, scalastic, findbugs
  ), settings = Defaults.defaultSettings ++ buildSettings).settings(
    scalaVersion := "2.10.0-RC3",
    templatesImport ++= Seq(
      "lila.game.{ DbGame, DbPlayer, Pov }",
      "lila.user.User",
      "lila.security.Permission",
      "lila.templating.Environment._",
      "lila.ui",
      "lila.http.Context",
      "com.github.ornicar.paginator.Paginator"),
    resolvers ++= Seq(iliaz, sonatype, sonatypeS, typesafe, t2v, guice, jgitMaven, christophs)
  ) dependsOn scalachess aggregate scalachess

  lazy val scalachess = Project("scalachess", file("scalachess"), settings = Project.defaultSettings ++ buildSettings).settings(
    resolvers := Seq(iliaz, sonatype),
    scalaVersion := "2.10.0-RC3",
    libraryDependencies := Seq(scalaz, scalalib, hasher, jodaTime, jodaConvert)
  )

  lazy val cli = Project("cli", file("cli"), settings = buildSettings).settings(
    libraryDependencies ++= Seq(scalastic)
  ) dependsOn lila
}
