import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {
    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val sonatype = "sonatype" at "https://oss.sonatype.org/content/repositories/releases"
    val sonatypeS = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
    val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
    val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
    val sprayRepo = "spray repo" at "http://repo.spray.io"
    val prismic = "Prismic.io kits" at "https://s3.amazonaws.com/prismic-maven-kits/repository/maven/"

    val commons = Seq(
      sonatypeS,
      sonatype,
      awesomepom,
      typesafe,
      prismic,
      t2v, jgitMaven, sprayRepo)
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.7"
  val scalalib = "com.github.ornicar" %% "scalalib" % "5.4"
  val config = "com.typesafe" % "config" % "1.3.0"
  val apache = "org.apache.commons" % "commons-lang3" % "3.4"
  val guava = "com.google.guava" % "guava" % "19.0"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.1"
  val hasher = "com.roundeights" %% "hasher" % "1.2.0"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "3.2.0.201312181205-r"
  val jodaTime = "joda-time" % "joda-time" % "2.9.1"
  val RM = "org.reactivemongo" %% "reactivemongo" % "0.11.9"
  val PRM = "org.reactivemongo" %% "play2-reactivemongo" % "0.11.9"
  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.2.3-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.2.11-THIB"

  object play {
    val version = "2.4.6"
    val api = "com.typesafe.play" %% "play" % version
    val test = "com.typesafe.play" %% "play-test" % version
  }
  object spray {
    val version = "1.3.3"
    val caching = "io.spray" %% "spray-caching" % version
    val util = "io.spray" %% "spray-util" % version
  }
  object akka {
    val version = "2.4.1"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
  }
}
