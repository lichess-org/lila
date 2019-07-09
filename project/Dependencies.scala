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
  val jodaTime = "joda-time" % "joda-time" % "2.10.2"
  val chess = "org.lichess" %% "scalachess" % "8.6.22"
  val compression = "org.lichess" %% "compression" % "1.4"
  val maxmind = "com.sanoma.cda" %% "maxmind-geoip2-scala" % "1.2.3-THIB"
  val prismic = "io.prismic" %% "scala-kit" % "1.2.13-THIB211"
  val java8compat = "org.scala-lang.modules" %% "scala-java8-compat" % "0.9.0"
  val semver = "com.gilt" %% "gfc-semver" % "0.0.5"
  val scrimage = "com.sksamuel.scrimage" %% "scrimage-core" % "2.1.8"
  val scalaConfigs = "com.github.kxbmap" %% "configs" % "0.4.4"
  val scaffeine = "com.github.blemale" %% "scaffeine" % "2.6.0" % "compile"
  val netty = "io.netty" % "netty" % "3.10.6.Final"
  val guava = "com.google.guava" % "guava" % "21.0"
  val specs2 = "org.specs2" %% "specs2-core" % "4.0.2" % "test"
  val specs2Scalaz = "org.specs2" %% "specs2-scalaz" % "4.0.2" % "test"
  val scalaUri = "io.lemonlabs" %% "scala-uri" % "1.2.0"
  val scalatags = "com.lihaoyi" %% "scalatags" % "0.6.7"
  val lettuce = "io.lettuce" % "lettuce-core" % "5.1.7.RELEASE"
  val epoll = "io.netty" % "netty-transport-native-epoll" % "4.1.36.Final" classifier "linux-x86_64"

  object reactivemongo {
    val version = "0.12.4"
    val driver = ("org.reactivemongo" %% "reactivemongo" % version)
      .exclude("com.typesafe.akka", "*") // provided by Play
      .exclude("com.typesafe.play", "*")
    val iteratees = ("org.reactivemongo" %% "reactivemongo-iteratees" % version)
      .exclude("com.typesafe.akka", "*") // provided by Play
      .exclude("com.typesafe.play", "*")
  }

  object play {
    val version = "2.4.11"
    val api = "com.typesafe.play" %% "play" % version
    val test = "com.typesafe.play" %% "play-test" % version
  }
  object akka {
    val version = "2.4.20"
    val actor = "com.typesafe.akka" %% "akka-actor" % version
    val slf4j = "com.typesafe.akka" %% "akka-slf4j" % version
  }
  object kamon {
    val version = "0.6.4.2-LILA"
    val core = "io.kamon" %% "kamon-core" % version
    val influxdb = "io.kamon" %% "kamon-influxdb" % version
  }
}
