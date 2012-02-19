import sbt._
import Keys._
import PlayProject._

trait Resolvers {
  val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
  //val twitter = "twitter.com" at "http://maven.twttr.com/"
  val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
  val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
  val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
}

trait Dependencies {
  val twitterUtilVersion = "1.12.12"

  val twitterCore = "com.twitter" % "util-core" % twitterUtilVersion
  val slf4jNop = "org.slf4j" % "slf4j-nop" % "1.6.4"
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val specs2 = "org.specs2" %% "specs2" % "1.8"
  val redis = "net.debasishg" %% "redisclient" % "2.4.2"
  val json = "net.liftweb" %% "lift-json" % "2.4-RC1"
  val casbah = "com.mongodb.casbah" %% "casbah" % "2.1.5-1"
  val salat = "com.novus" %% "salat-core" % "0.0.8-SNAPSHOT"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  val appVersion = "1.0-SNAPSHOT"

  val lila = Project("lila", file("lila")).settings(
    libraryDependencies := Seq(
      scalaz, specs2, redis, json, slf4jNop, casbah, salat
    ),
    resolvers := Seq(
      codahale, typesafe, iliaz, sonatype
    ),
    shellPrompt := ShellPrompt.buildShellPrompt
  )

  val main = PlayProject("app", appVersion, Seq(), mainLang = SCALA) dependsOn lila
}

object ShellPrompt {

  val buildShellPrompt = {
    (state: State) ⇒
      {
        val currProject = Project.extract(state).currentProject.id
        "%s> ".format(currProject)
      }
  }
}
