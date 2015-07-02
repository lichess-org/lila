import sbt._, Keys._

object Dependencies {

  private val home = "file://" + Path.userHome.absolutePath

  object Resolvers {
    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val sonatype = "sonatype" at "https://oss.sonatype.org/content/repositories/releases"
    val sonatypeS = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
    val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
    val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
    val sprayRepo = "spray repo" at "http://repo.spray.io"
    val roundeights = "RoundEights" at "http://maven.spikemark.net/roundeights"
    val prismic = "Prismic.io kits" at "https://s3.amazonaws.com/prismic-maven-kits/repository/maven/"

    val commons = Seq(
      sonatypeS,
      sonatype,
      awesomepom,
      typesafe,
      roundeights,
      prismic,
      t2v, jgitMaven, sprayRepo)
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.1.2"
  val scalalib = "com.github.ornicar" %% "scalalib" % "5.3"
  val config = "com.typesafe" % "config" % "1.3.0"
  val apache = "org.apache.commons" % "commons-lang3" % "3.4"
  val guava = "com.google.guava" % "guava" % "18.0"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "2.0.3"
  val hasher = "com.roundeights" %% "hasher" % "1.0.0"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "3.2.0.201312181205-r"
  val jodaTime = "joda-time" % "joda-time" % "2.7"
  val elastic4s = "com.sksamuel.elastic4s" %% "elastic4s" % "1.5.10"
  val RM = "org.reactivemongo" %% "reactivemongo" % "0.10.5.0.akka23"
  val PRM = "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23"
  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.2.3-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.3.3"

  object play {
    val version = "2.3.9"
    val api = "com.typesafe.play" %% "play" % version
    val test = "com.typesafe.play" %% "play-test" % version
  }
  object spray {
    val version = "1.3.3"
    val caching = "io.spray" %% "spray-caching" % version
    val util = "io.spray" %% "spray-util" % version
  }
}
