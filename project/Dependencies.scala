import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    val sonatype  = Resolver.sonatypeOssRepos("releases")
    val sonatypeS = Resolver.sonatypeOssRepos("snapshots")
    val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/lichess-org/lila-maven/master"

    val commons = Seq(lilaMaven) ++ sonatype ++ sonatypeS
  }

  val shogi       = "io.github.WandererXII" %% "scalashogi"                     % "12.1.1"
  val scalalib    = "com.github.ornicar"   %% "scalalib"                        % "7.0.2"
  val hasher      = "com.roundeights"      %% "hasher"                          % "1.2.1"
  val jodaTime    = "joda-time"             % "joda-time"                       % "2.12.7"
  val maxmind     = "com.sanoma.cda"       %% "maxmind-geoip2-scala"            % "1.3.1-THIB"
  val prismic     = "io.prismic"           %% "scala-kit"                       % "1.2.18-THIB213"
  val scrimage    = "com.sksamuel.scrimage" % "scrimage-core"                   % "4.0.22"
  val scaffeine   = "com.github.blemale"   %% "scaffeine"                       % "5.2.1" % "compile"
  val googleOAuth = "com.google.auth"       % "google-auth-library-oauth2-http" % "1.23.0"
  val galimatias  = "io.mola.galimatias"    % "galimatias"                      % "0.2.2-NF"
  val scalatags   = "com.lihaoyi"          %% "scalatags"                       % "0.13.1"
  val lettuce     = "io.lettuce"            % "lettuce-core"                    % "6.3.2.RELEASE"
  val epoll       = "io.netty"              % "netty-transport-native-epoll"    % "4.1.111.Final" classifier "linux-x86_64"
  val autoconfig  = "io.methvin.play"      %% "autoconfig-macros"               % "0.3.2" % "provided"
  val scalatest   = "org.scalatest"        %% "scalatest"                       % "3.2.18" % Test
  val uaparser    = "org.uaparser"         %% "uap-scala"                       % "0.17.0"

  object flexmark {
    val version = "0.50.50"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("formatter", "ext-tables", "ext-autolink", "ext-gfm-strikethrough").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val version = "2.5.9"
    val macros = "com.softwaremill.macwire" %% "macros" % version % "provided"
    val util   = "com.softwaremill.macwire" %% "util"   % version % "provided"
  }

  object reactivemongo {
    val version = "1.0.10"

    val driver     = "org.reactivemongo" %% "reactivemongo"               % version
    val stream     = "org.reactivemongo" %% "reactivemongo-akkastream"    % version
    val epoll      = "org.reactivemongo"  % "reactivemongo-shaded-native" % s"$version-linux-x86-64"
    def bundle     = Seq(driver, stream)
  }

  object play {
    val version = "2.9.4"
    val api     = "com.typesafe.play" %% "play"      % version
    val json    = "com.typesafe.play" %% "play-json" % "2.10.6"
  }
  object kamon {
    val version    = "2.2.3"
    val core       = "io.kamon" %% "kamon-core"           % version
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % version
    val metrics    = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus"     % version
  }
  object akka {
    val version    = "2.6.21"
    val akka       = "com.typesafe.akka" %% "akka-actor"       % version
    val akkaTyped  = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream"      % version
    val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"       % version
    val testkit    = "com.typesafe.akka" %% "akka-testkit"     % version % Test
    def bundle     = List(akka, akkaTyped, akkaStream, akkaSlf4j)
  }
}
