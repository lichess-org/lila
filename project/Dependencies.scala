import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    // val typesafe = Resolver.typesafeRepo("releases")
    val sonatype = Resolver.sonatypeRepo("releases")
    // val sonatypeS = "sonatype snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
    // val awesomepom = "awesomepom" at "https://raw.githubusercontent.com/jibs/maven-repo-scala/master"
    val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

    val commons = Seq(
      // sonatypeS,
      lilaMaven,
      sonatype,
      // awesomepom,
      // typesafe,
      // prismic
    )
  }

  val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.29"
  val scalalib = "com.github.ornicar" %% "scalalib" % "6.7"
  // val findbugs = "com.google.code.findbugs" % "jsr305" % "3.0.1"
  val hasher = "com.roundeights" %% "hasher" % "1.2.1"
  val jodaTime = "joda-time" % "joda-time" % "2.10.5"
  val chess = "org.lichess" %% "scalachess" % "9.0.27"
  val compression = "org.lichess" %% "compression" % "1.5"
  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.3.1-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.2.13-THIB213"
  val scrimage = "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8-SNAPSHOT"
  val scaffeine = "com.github.blemale" %% "scaffeine" % "3.1.0" % "compile"
  val googleOAuth = "com.google.auth" % "google-auth-library-oauth2-http" % "0.18.0"
  // val specs2 = "org.specs2" %% "specs2-core" % "4.0.2" % "test"
  // val specs2Scalaz = "org.specs2" %% "specs2-scalaz" % "4.0.2" % "test"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "1.5.1"
  val scalatags = "com.lihaoyi" %% "scalatags" % "0.7.0"
  val lettuce = "io.lettuce" % "lettuce-core" % "5.2.1.RELEASE"
  val epoll = "io.netty" % "netty-transport-native-epoll" % "4.1.43.Final" classifier "linux-x86_64"
  val markdown = "com.vladsch.flexmark" % "flexmark-all" % "0.50.44"
  val autoconfig = "io.methvin.play" %% "autoconfig-macros" % "0.3.0"

  object macwire {
    val version = "2.3.3"
    val macros = "com.softwaremill.macwire" %% "macros" % version
    val util = "com.softwaremill.macwire" %% "util" % version
  }

  object reactivemongo {
    val version = "0.19.2"
    val driver = "org.reactivemongo" %% "reactivemongo" % version
    val bson = "org.reactivemongo" %% "reactivemongo-bson-api" % version
    val native = "org.reactivemongo" % "reactivemongo-shaded-native" % s"$version-linux-x86-64" % "runtime" classifier "linux-x86_64"
    // #TODO remove compat
    val compat = "org.reactivemongo" %% "reactivemongo-bson-compat" % version
    def bundle = Seq(driver, bson, compat)
  }

  object play {
    val version = "2.8.0-RC2"
    val libVersion = "2.8.0"
    val api = "com.typesafe.play" %% "play" % version
    val json = "com.typesafe.play" %% "play-json" % libVersion
    val joda = "com.typesafe.play" %% "play-json-joda" % libVersion
    // val test = "com.typesafe.play" %% "play-test" % version
  }
  object kamon {
    val core = "io.kamon" %% "kamon-core" % "2.0.1"
    val influxdb = "io.kamon" %% "kamon-influxdb" % "2.0.0"
    val metrics = "io.kamon" %% "kamon-system-metrics" % "2.0.0"
  }
}
