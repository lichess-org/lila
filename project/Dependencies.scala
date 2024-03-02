import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {
  val arch     = if (System.getProperty("os.arch").toLowerCase.startsWith("aarch")) "aarch_64" else "x86_64"
  val dashArch = arch.replace("_", "-")
  val (os, notifier) =
    if (System.getProperty("os.name").toLowerCase.startsWith("mac"))
      ("osx", "kqueue")
    else
      ("linux", "epoll")

  val lilaMaven = "lila-maven".at("https://raw.githubusercontent.com/lichess-org/lila-maven/master")
  val sonashots = "sonashots".at("https://oss.sonatype.org/content/repositories/snapshots")

  val cats        = "org.typelevel"                %% "cats-core"                       % "2.10.0"
  val alleycats   = "org.typelevel"                %% "alleycats-core"                  % "2.10.0"
  val scalalib    = "com.github.ornicar"           %% "scalalib"                        % "9.5.6"
  val hasher      = "com.roundeights"              %% "hasher"                          % "1.3.1"
  val chess       = "org.lichess"                  %% "scalachess"                      % "15.7.12"
  val compression = "org.lichess"                  %% "compression"                     % "1.10"
  val maxmind     = "com.maxmind.geoip2"            % "geoip2"                          % "4.0.1"
  val caffeine    = "com.github.ben-manes.caffeine" % "caffeine"                        % "3.1.8" % "compile"
  val scaffeine   = "com.github.blemale"           %% "scaffeine"                       % "5.2.1" % "compile"
  val googleOAuth = "com.google.auth"               % "google-auth-library-oauth2-http" % "1.23.0"
  val galimatias  = "io.mola.galimatias"            % "galimatias"                      % "0.2.2-NF"
  val scalatags   = "com.lihaoyi"                  %% "scalatags"                       % "0.12.0"
  val lettuce     = "io.lettuce"                    % "lettuce-core"                    % "6.3.1.RELEASE"
  val nettyTransport =
    ("io.netty" % s"netty-transport-native-$notifier" % "4.1.107.Final").classifier(s"$os-$arch")
  val munit       = "org.scalameta"              %% "munit"         % "1.0.0-M11" % Test
  val uaparser    = "org.uaparser"               %% "uap-scala"     % "0.16.0"
  val apacheText  = "org.apache.commons"          % "commons-text"  % "1.11.0"
  val apacheMath  = "org.apache.commons"          % "commons-math3" % "3.6.1"
  val bloomFilter = "com.github.alexandrnikitin" %% "bloom-filter"  % "0.13.1_lila-1"
  val kittens     = "org.typelevel"              %% "kittens"       % "3.2.0"

  object tests {
    val bundle = Seq(munit)
  }

  object flexmark {
    val version = "0.64.8"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("ext-tables", "ext-autolink", "ext-gfm-strikethrough", "html2md-converter").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val version = "2.5.9"
    val macros  = "com.softwaremill.macwire" %% "macros"  % version % "provided"
    val util    = "com.softwaremill.macwire" %% "util"    % version % "provided"
    val tagging = "com.softwaremill.common"  %% "tagging" % "2.3.4"
    def bundle  = Seq(macros, util, tagging)
  }

  object reactivemongo {
    val driver = "org.reactivemongo" %% "reactivemongo"                              % "1.1.0-RC12"
    val stream = "org.reactivemongo" %% "reactivemongo-akkastream"                   % "1.1.0-RC12"
    val shaded = "org.reactivemongo"  % s"reactivemongo-shaded-native-$os-$dashArch" % "1.1.0-RC12"
    // val kamon  = "org.reactivemongo" %% "reactivemongo-kamon"         % "1.0.8"
    def bundle = Seq(driver, stream)
  }

  object play {
    val playVersion = "2.9.2"
    val json        = "org.playframework" %% "play-json"         % "3.0.2"
    val api         = "com.typesafe.play" %% "play"              % playVersion
    val server      = "com.typesafe.play" %% "play-server"       % playVersion
    val netty       = "com.typesafe.play" %% "play-netty-server" % playVersion
    val logback     = "com.typesafe.play" %% "play-logback"      % playVersion
    val mailer      = "org.playframework" %% "play-mailer"       % "10.0.0"
  }

  object playWs {
    val version = "2.2.5"
    val ahc     = "com.typesafe.play" %% "play-ahc-ws-standalone"  % version
    val json    = "com.typesafe.play" %% "play-ws-standalone-json" % version
    val bundle  = Seq(ahc, json)
  }

  object kamon {
    val version    = "2.7.0"
    val core       = "io.kamon" %% "kamon-core"           % version
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % version
    val metrics    = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus"     % version
  }
  object akka {
    val version    = "2.6.20"
    val actor      = "com.typesafe.akka" %% "akka-actor"       % version
    val actorTyped = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream"      % version
    val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"       % version
    val testkit    = "com.typesafe.akka" %% "akka-testkit"     % version % Test
    def bundle     = List(actor, actorTyped, akkaStream, akkaSlf4j)
  }
}
