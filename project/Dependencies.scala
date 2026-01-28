import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {
  val arch = if (System.getProperty("os.arch").toLowerCase.startsWith("aarch")) "aarch_64" else "x86_64"
  val dashArch = arch.replace("_", "-")
  val (os, notifier) =
    if (System.getProperty("os.name").toLowerCase.startsWith("mac"))
      ("osx", "kqueue")
    else
      ("linux", "epoll")

  val jitpack = "jitpack".at("https://jitpack.io")
  val lilaMaven = "lila-maven".at("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
  val sonashots = "sonashots".at("https://oss.sonatype.org/content/repositories/snapshots")

  val cats = "org.typelevel" %% "cats-core" % "2.13.0"
  val alleycats = "org.typelevel" %% "alleycats-core" % "2.13.0"
  val catsMtl = "org.typelevel" %% "cats-mtl" % "1.6.0"
  val hasher = "com.roundeights" %% "hasher" % "1.3.1"
  val compression = "com.github.lichess-org" % "compression" % "3.2.0"
  val maxmind = "com.maxmind.geoip2" % "geoip2" % "4.0.1"
  val caffeine = "com.github.ben-manes.caffeine" % "caffeine" % "3.2.3" % "compile"
  val scaffeine = "com.github.blemale" %% "scaffeine" % "5.3.0" % "compile"
  val googleOAuth = "com.google.auth" % "google-auth-library-oauth2-http" % "1.41.0"
  val galimatias = "io.mola.galimatias" % "galimatias" % "0.2.2-NF"
  val scalatags = "com.lihaoyi" %% "scalatags" % "0.13.1"
  val lettuce = "io.lettuce" % "lettuce-core" % "7.2.1.RELEASE"
  val nettyTransport =
    ("io.netty" % s"netty-transport-native-$notifier" % "4.2.9.Final").classifier(s"$os-$arch")
  val lilaSearch = "com.github.lichess-org.lila-search" %% "client" % "3.3.0"
  val munit = "org.scalameta" %% "munit" % "1.2.1" % Test
  val uaparser = "org.uaparser" %% "uap-scala" % "0.21.0"
  val apacheText = "org.apache.commons" % "commons-text" % "1.15.0"
  val apacheMath = "org.apache.commons" % "commons-math3" % "3.6.1"
  val bloomFilter = "com.github.alexandrnikitin" %% "bloom-filter" % "0.13.1_lila-1"
  val kittens = "org.typelevel" %% "kittens" % "3.5.0"

  val scalacheck = "org.scalacheck" %% "scalacheck" % "1.19.0" % Test
  val munitCheck = "org.scalameta" %% "munit-scalacheck" % "1.2.0" % Test

  object tests {
    val bundle = Seq(munit)
  }

  object chess {
    val version = "17.14.3"
    val org = "com.github.lichess-org.scalachess"
    // val org = "org.lichess" // for publishLocal
    val core = org %% "scalachess" % version
    val testKit = org %% "scalachess-test-kit" % version % Test
    val playJson = org %% "scalachess-play-json" % version
    val rating = org %% "scalachess-rating" % version
    val tiebreak = org %% "scalachess-tiebreak" % version
    def bundle = Seq(core, testKit, playJson, rating, tiebreak)
  }

  object scalalib {
    val version = "11.9.5"
    val org = "com.github.lichess-org.scalalib"
    // val org = "org.lichess" // for publishLocal
    val core = org %% "scalalib-core" % version
    val model = org %% "scalalib-model" % version
    val playJson = org %% "scalalib-play-json" % version
    val lila = org %% "scalalib-lila" % version
    def bundle = Seq(core, model, playJson, lila)
  }

  object flexmark {
    val version = "0.64.8"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("ext-tables", "ext-anchorlink", "ext-autolink", "ext-gfm-strikethrough", "html2md-converter")
          .map { ext =>
            "com.vladsch.flexmark" % s"flexmark-$ext" % version
          }
  }

  object macwire {
    val version = "2.6.7"
    val macros = "com.softwaremill.macwire" %% "macros" % version % "provided"
    val util = "com.softwaremill.macwire" %% "util" % version % "provided"
    val tagging = "com.softwaremill.common" %% "tagging" % "2.3.5"
    def bundle = Seq(macros, util, tagging)
  }

  object reactivemongo {
    val rmVersion = "1.1.0-RC19"
    val driver = "org.reactivemongo" %% "reactivemongo" % rmVersion
    val stream = "org.reactivemongo" %% "reactivemongo-akkastream" % rmVersion
    val shaded = "org.reactivemongo" % s"reactivemongo-shaded-native-$os-$dashArch" % rmVersion
    // val kamon  = "org.reactivemongo" %% "reactivemongo-kamon"         % "1.0.8"
    def bundle = Seq(driver, stream)
  }

  object play {
    val playVersion = "2.8.18-lila_3.22"
    val json = "org.playframework" %% "play-json" % "3.0.6"
    val api = "com.typesafe.play" %% "play" % playVersion
    val server = "com.typesafe.play" %% "play-server" % playVersion
    val netty = "com.typesafe.play" %% "play-netty-server" % playVersion
    val logback = "com.typesafe.play" %% "play-logback" % playVersion
    val mailer = "org.playframework" %% "play-mailer" % "10.1.0"
  }

  object playWs {
    val version = "2.2.14"
    val ahc = "com.typesafe.play" %% "play-ahc-ws-standalone" % version
    val json = "com.typesafe.play" %% "play-ws-standalone-json" % version
    val bundle = Seq(ahc, json)
  }

  object kamon {
    val version = "2.8.0"
    val core = "io.kamon" %% "kamon-core" % version
    val influxdb = "io.kamon" %% "kamon-influxdb" % version
    val metrics = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus" % version
  }
  object akka {
    val version = "2.6.21"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val actorTyped = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream" % version
    val akkaSlf4j = "com.typesafe.akka" %% "akka-slf4j" % version
    val testkit = "com.typesafe.akka" %% "akka-testkit" % version % Test
    def bundle = List(actor, actorTyped, akkaStream, akkaSlf4j)
  }
}
