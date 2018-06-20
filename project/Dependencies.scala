import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
    val sonatype = "sonatype" at "https://oss.sonatype.org/content/repositories/releases"
    val sonatypeS = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    val awesomepom = "awesomepom" at "https://raw.githubusercontent.com/jibs/maven-repo-scala/master"
    val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"
    val prismic = "Prismic.io kits" at "https://s3.amazonaws.com/prismic-maven-kits/repository/maven/"

    val commons = Seq(
      sonatypeS,
      lilaMaven,
      sonatype,
      awesomepom,
      typesafe,
      prismic
    )
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.16"
  val scalalib = "com.github.ornicar" %% "scalalib" % "6.6"
  val typesafeConfig = "com.typesafe" % "config" % "1.3.1"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.1"
  val hasher = "com.roundeights" %% "hasher" % "1.2.0"
  val jodaTime = "joda-time" % "joda-time" % "2.9.9"
  val chess = "org.lichess" %% "scalachess" % "8.6.11"
  val compression = "org.lichess" %% "compression" % "1.4"
  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.3.0-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.2.12-THIB"
  val semver = "com.gilt" %% "gfc-semver" % "0.0.5"
  val scrimage = "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8"
  val scalaConfigs = "com.github.kxbmap" %% "configs" % "0.4.4"
  val scaffeine = "com.github.blemale" %% "scaffeine" % "2.5.0" % "compile"
  val guava = "com.google.guava" % "guava" % "21.0"
  val specs2 = "org.specs2" %% "specs2-core" % "4.0.2" % "test"
  val specs2Scalaz = "org.specs2" %% "specs2-scalaz" % "4.0.2" % "test"

  object reactivemongo {
    val version = "0.12.3"
    val driver = ("org.reactivemongo" %% "reactivemongo" % version)
      .exclude("com.typesafe.akka", "*") // provided by Play
      .exclude("com.typesafe.play", "*")
    val iteratees = ("org.reactivemongo" %% "reactivemongo-iteratees" % version)
      .exclude("com.typesafe.akka", "*") // provided by Play
      .exclude("com.typesafe.play", "*")
  }

  object play {
    val version = "2.6.15"
    val api = "com.typesafe.play" %% "play" % version
    val jodaForms = "com.typesafe.play" %% "play-joda-forms" % version
    val jodaJson = "com.typesafe.play" %% "play-json-joda" % "2.6.9"
    val test = "com.typesafe.play" %% "play-test" % version
    val iteratee = "com.typesafe.play" %% "play-iteratees" % "2.6.1"
    val streams = "com.typesafe.play" %% "play-iteratees-reactive-streams" % "2.6.1"
  }
  object akka {
    val version = "2.5.13"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    // val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
  }
  object kamon {
    val core = "io.kamon" %% "kamon-core" % "1.1.2"
    val influxdb = "io.kamon" %% "kamon-influxdb" % "1.0.1"
  }
}
