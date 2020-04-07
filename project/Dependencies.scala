import play.sbt.PlayImport._
import sbt._, Keys._

object Dependencies {

  object Resolvers {

    val sonatype  = Resolver.sonatypeRepo("releases")
    val sonatypeS = Resolver.sonatypeRepo("snapshots")
    val lilaMaven = "lila-maven" at "https://raw.githubusercontent.com/ornicar/lila-maven/master"

    val commons = Seq(sonatype, lilaMaven, sonatypeS)
  }

  val scalaz      = "org.scalaz"            %% "scalaz-core"                    % "7.2.30"
  val scalalib    = "com.github.ornicar"    %% "scalalib"                       % "6.8"
  val hasher      = "com.roundeights"       %% "hasher"                         % "1.2.1"
  val jodaTime    = "joda-time"             % "joda-time"                       % "2.10.5"
  val chess       = "org.lichess"           %% "scalachess"                     % "9.2.1"
  val compression = "org.lichess"           %% "compression"                    % "1.5"
  val maxmind     = "com.sanoma.cda"        %% "maxmind-geoip2-scala"           % "1.3.1-THIB"
  val prismic     = "io.prismic"            %% "scala-kit"                      % "1.2.18-THIB213"
  val scrimage    = "com.sksamuel.scrimage" %% "scrimage-core"                  % "2.1.8-SNAPSHOT"
  val scaffeine   = "com.github.blemale"    %% "scaffeine"                      % "4.0.0" % "compile"
  val googleOAuth = "com.google.auth"       % "google-auth-library-oauth2-http" % "0.20.0"
  val scalaUri    = "io.lemonlabs"          %% "scala-uri"                      % "2.2.0"
  val scalatags   = "com.lihaoyi"           %% "scalatags"                      % "0.8.5"
  val lettuce     = "io.lettuce"            % "lettuce-core"                    % "5.2.2.RELEASE"
  val epoll       = "io.netty"              % "netty-transport-native-epoll"    % "4.1.44.Final" classifier "linux-x86_64"
  val autoconfig  = "io.methvin.play"       %% "autoconfig-macros"              % "0.3.2" % "provided"
  val scalatest   = "org.scalatest"         %% "scalatest"                      % "3.1.0" % Test
  val akkatestkit = "com.typesafe.akka"     %% "akka-testkit"                   % "2.6.1" % Test

  object flexmark {
    val version = "0.50.50"
    val bundle =
      ("com.vladsch.flexmark" % "flexmark" % version) ::
        List("formatter", "ext-tables", "ext-autolink", "ext-gfm-strikethrough").map { ext =>
          "com.vladsch.flexmark" % s"flexmark-$ext" % version
        }
  }

  object macwire {
    val version = "2.3.3"
    val macros  = "com.softwaremill.macwire" %% "macros" % version % "provided"
    val util    = "com.softwaremill.macwire" %% "util" % version % "provided"
  }

  object reactivemongo {
    val version = "0.20.3"
    val driver  = "org.reactivemongo" %% "reactivemongo" % version
    val stream  = "org.reactivemongo" %% "reactivemongo-akkastream" % version
    val epoll   = "org.reactivemongo" % "reactivemongo-shaded-native" % s"$version-linux-x86-64"
    def bundle  = Seq(driver, stream)
  }

  object play {
    val version = "2.8.1"
    val api     = "com.typesafe.play" %% "play" % version
    val json    = "com.typesafe.play" %% "play-json" % "2.8.1"
  }
  object kamon {
    val core       = "io.kamon" %% "kamon-core"           % "2.1.0"
    val influxdb   = "io.kamon" %% "kamon-influxdb"       % "2.1.0"
    val metrics    = "io.kamon" %% "kamon-system-metrics" % "2.1.0"
    val prometheus = "io.kamon" %% "kamon-prometheus"     % "2.1.0"
  }

  object silencer {
    val version = "1.4.4"
    val plugin  = "com.github.ghik" % "silencer-plugin" % version cross CrossVersion.full
    val lib     = "com.github.ghik" % "silencer-lib" % version % Provided cross CrossVersion.full
    val bundle  = Seq(compilerPlugin(plugin), lib)
  }
}
