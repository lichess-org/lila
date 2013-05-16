import sbt._
import Keys._

object Dependencies {

  private val home = "file://" + Path.userHome.absolutePath

  object Resolvers {
    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
    val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
    val sonatypeS = "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
    val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
    val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
    val christophs = "Christophs Maven Repo" at "http://maven.henkelmann.eu/"
    val sgodbillon = "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/"
    val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
    val sprayRepo = "spray repo" at "http://repo.spray.io"
    val localSonatype = "local sonatype repo" at home + "/local-repo/sonatype/snapshots"
    val local = "local repo" at home + "/local-repo"

    val commons = Seq(
      local,
      localSonatype,
      sonatypeS,
      sonatype,
      awesomepom, iliaz,
      typesafe,
      t2v, jgitMaven, christophs, sprayRepo)
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val scalalib = "com.github.ornicar" %% "scalalib" % "3.3"
  val config = "com.typesafe" % "config" % "1.0.0"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "org.scala-tools.time" % "time_2.9.1" % "0.5"
  val guava = "com.google.guava" % "guava" % "14.0.1"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "2.0.1"
  val csv = "com.github.tototoshi" %% "scala-csv" % "0.8.0"
  val hasher = "hasher" %% "hasher" % "0.3.1"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "2.3.1.201302201838-r"
  val actuarius = "eu.henkelmann" %% "actuarius" % "0.2.6-THIB"
  val jodaTime = "joda-time" % "joda-time" % "2.2"
  val jodaConvert = "org.joda" % "joda-convert" % "1.3.1"
  val scalastic = "scalastic" %% "scalastic" % "0.90.0-thib"
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % "0.9"
  val playReactivemongo = "org.reactivemongo" %% "play2-reactivemongo" % "0.9"
  object play {
    val version = "2.1.2-RC1"
    val api = "play" %% "play" % version
    val test = "play" %% "play-test" % version
  }
  object akka {
    val version = "2.1.0"
    val agent = "com.typesafe.akka" %% "akka-agent" % version
  }
  object spray {
    val version = "1.1-M7"
    val caching = "io.spray" % "spray-caching" % version
    val util = "io.spray" % "spray-util" % version
  }
}
