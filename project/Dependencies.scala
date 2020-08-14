import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    val sonatype  = Resolver.sonatypeRepo("releases")
    val sonatypeS = Resolver.sonatypeRepo("snapshots")
    val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

    val commons = Seq(sonatype, lilaMaven, sonatypeS)
  }

  val scalaz      = "org.scalaz"           %% "scalaz-core"                     % "7.2.30"
  val scalalib    = "com.github.ornicar"   %% "scalalib"                        % "6.8"
  val hasher      = "com.roundeights"      %% "hasher"                          % "1.2.1"
  val jodaTime    = "joda-time"             % "joda-time"                       % "2.10.6"
  //val chess       = "org.lichess"          %% "scalachess"                      % "9.3.1"
  //val compression = "org.lichess"          %% "compression"                     % "1.5"
  val maxmind     = "com.sanoma.cda"       %% "maxmind-geoip2-scala"            % "1.3.1-THIB"
  val prismic     = "io.prismic"           %% "scala-kit"                       % "1.2.18-THIB213"
  val scrimage    = "com.sksamuel.scrimage" % "scrimage-core"                   % "4.0.5"
  val scaffeine   = "com.github.blemale"   %% "scaffeine"                       % "4.0.1" % "compile"
  val googleOAuth = "com.google.auth"       % "google-auth-library-oauth2-http" % "0.21.1"
  val scalaUri    = "io.lemonlabs"         %% "scala-uri"                       % "2.2.3"
  val scalatags   = "com.lihaoyi"          %% "scalatags"                       % "0.8.5"
  val lettuce     = "io.lettuce"            % "lettuce-core"                    % "5.3.1.RELEASE"
  val epoll       = "io.netty"              % "netty-transport-native-epoll"    % "4.1.51.Final" classifier "linux-x86_64"
  val autoconfig  = "io.methvin.play"      %% "autoconfig-macros"               % "0.3.2" % "provided"
  val scalatest   = "org.scalatest"        %% "scalatest"                       % "3.1.0" % Test
  val uaparser    = "org.uaparser"         %% "uap-scala"                       % "0.11.0"

  object flexmark {
    val version = "0.50.50"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("formatter", "ext-tables", "ext-autolink", "ext-gfm-strikethrough").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val macros = "com.softwaremill.macwire" %% "macros" % "2.3.7" % "provided"
    val util   = "com.softwaremill.macwire" %% "util"   % "2.3.7" % "provided"
  }

  object reactivemongo {
    val versionFix = "0.20.12-fix1"
    val version    = "0.20.12"
    val driver     = "org.reactivemongo" %% "reactivemongo"               % versionFix
    val stream     = "org.reactivemongo" %% "reactivemongo-akkastream"    % version
    val epoll      = "org.reactivemongo"  % "reactivemongo-shaded-native" % s"$version-linux-x86-64"
    def bundle     = Seq(driver, stream)
  }

  object play {
    val version = "2.8.2"
    val api     = "com.typesafe.play" %% "play"      % version
    val json    = "com.typesafe.play" %% "play-json" % "2.9.0"
  }
  object kamon {
    val version    = "2.1.3"
    val core       = "io.kamon" %% "kamon-core"           % version
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % version
    val metrics    = "io.kamon" %% "kamon-system-metrics" % version
    val prometheus = "io.kamon" %% "kamon-prometheus"     % version
  }
  object akka {
    val version    = "2.6.5"
    val akka       = "com.typesafe.akka" %% "akka-actor"       % version
    val akkaTyped  = "com.typesafe.akka" %% "akka-actor-typed" % version
    val akkaStream = "com.typesafe.akka" %% "akka-stream"      % version
    val akkaSlf4j  = "com.typesafe.akka" %% "akka-slf4j"       % version
    val testkit    = "com.typesafe.akka" %% "akka-testkit"     % version % Test
    def bundle     = List(akka, akkaTyped, akkaStream, akkaSlf4j)
  }
}
