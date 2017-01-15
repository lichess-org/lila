import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val sonatype = "sonatype" at "https://oss.sonatype.org/content/repositories/releases"
    // val sonatypeS = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
    val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
    val sprayRepo = "spray repo" at "http://repo.spray.io"
    val prismic = "Prismic.io kits" at "https://s3.amazonaws.com/prismic-maven-kits/repository/maven/"
    // val ornicarMaven = "ornicar maven" at "https://raw.githubusercontent.com/ornicar/maven/master/oss.sonatype.org/content/repositories/snapshots"

    val commons = Seq(
      // sonatypeS,
      // ornicarMaven,
      sonatype,
      awesomepom,
      typesafe,
      prismic,
      jgitMaven,
      sprayRepo)
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.11"
  val scalalib = "com.github.ornicar" %% "scalalib" % "5.7"
  val config = "com.typesafe" % "config" % "1.3.1"
  val apache = "org.apache.commons" % "commons-lang3" % "3.5"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.1"
  val hasher = "com.roundeights" %% "hasher" % "1.2.0"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "3.2.0.201312181205-r"
  val jodaTime = "joda-time" % "joda-time" % "2.9.7"

  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.2.3-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.2.11-THIB"
  val java8compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.8.0"
  val semver = "com.gilt" %% "gfc-semver" % "0.0.5"
  val scrimage = "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8"

  object reactivemongo {
    val version = "0.12.1"
    val driver = "org.reactivemongo" %% "reactivemongo" % version
    val iteratees = "org.reactivemongo" %% "reactivemongo-iteratees" % version
  }

  object play {
    val version = "2.4.6"
    val api = "com.typesafe.play" %% "play" % version
    val test = "com.typesafe.play" %% "play-test" % version
  }
  object spray {
    val version = "1.3.4"
    val caching = "io.spray" %% "spray-caching" % version
    val util = "io.spray" %% "spray-util" % version
  }
  object akka {
    val version = "2.4.16"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
  }
  object kamon {
    val version = "0.6.4.2-LILA"
    val core = "io.kamon" %% "kamon-core" % version
    val statsd = "io.kamon" %% "kamon-statsd" % version
    val influxdb = "io.kamon" %% "kamon-influxdb" % version
  }
}
