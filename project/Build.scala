import sbt._
import Keys._
import play.Project._

trait Resolvers {
  // val codahale = "repo.codahale.com" at "http://repo.codahale.com/"
  val typesafe = "typesafe.com" at "http://repo.typesafe.com/typesafe/releases/"
  val iliaz = "iliaz.com" at "http://scala.iliaz.com/"
  val sonatype = "sonatype" at "http://oss.sonatype.org/content/repositories/releases"
  val sonatypeS = "sonatype snapshots" at "http://oss.sonatype.org/content/repositories/snapshots"
  val t2v = "t2v.jp repo" at "http://www.t2v.jp/maven-repo/"
  val guice = "guice-maven" at "http://guice-maven.googlecode.com/svn/trunk"
  val jgitMaven = "jgit-maven" at "http://download.eclipse.org/jgit/maven"
  val christophs = "Christophs Maven Repo" at "http://maven.henkelmann.eu/"
  val sgodbillon = "sgodbillon" at "https://bitbucket.org/sgodbillon/repository/raw/master/snapshots/"
  val awesomepom = "awesomepom" at "https://raw.github.com/jibs/maven-repo-scala/master"
}

trait Dependencies {
  val scalaz = "org.scalaz" %% "scalaz-core" % "6.0.4"
  val salat = "com.novus" % "salat-core_2.9.2" % "1.9.1"
  val scalalib = "com.github.ornicar" %% "scalalib" % "3.3"
  val config = "com.typesafe" % "config" % "1.0.0"
  val guava = "com.google.guava" % "guava" % "13.0.1"
  val apache = "org.apache.commons" % "commons-lang3" % "3.1"
  val scalaTime = "org.scala-tools.time" % "time_2.9.1" % "0.5"
  val slf4jNop = "org.slf4j" %% "slf4j-nop" % "1.6.4"
  val paginator = "com.github.ornicar" % "paginator-core_2.9.1" % "1.6"
  val paginatorSalat = "com.github.ornicar" % "paginator-salat-adapter_2.9.1" % "1.5"
  val csv = "com.github.tototoshi" % "scala-csv_2.9.1" % "0.3"
  val hasher = "hasher" %% "hasher" % "0.3.1"
  val jgit = "org.eclipse.jgit" % "org.eclipse.jgit" % "1.3.0.201202151440-r"
  val actuarius = "eu.henkelmann" % "actuarius_2.9.2" % "0.2.4"
  val jodaTime = "joda-time" % "joda-time" % "2.1"
  val jodaConvert = "org.joda" % "joda-convert" % "1.2"
  val scalastic = "scalastic" % "scalastic_2.9.2" % "0.20.1-THIB"
  val findbugs = "com.google.code.findbugs" % "jsr305" % "1.3.+"
  val reactivemongoS = "org.reactivemongo" %% "reactivemongo" % "0.9-SNAPSHOT"
  val reactivemongo = "org.reactivemongo" %% "reactivemongo" % "0.8"
  val playReactivemongo = "play.modules.reactivemongo" %% "play2-reactivemongo" % "0.1-SNAPSHOT" cross CrossVersion.full
  val playProvided = "play" %% "play" % "2.1-SNAPSHOT" % "provided"
}

object ApplicationBuild extends Build with Resolvers with Dependencies {

  private val buildSettings = Defaults.defaultSettings ++ Seq(
    organization in ThisBuild := "org.lichess",
    scalaVersion in ThisBuild := "2.10.0",
    resolvers in ThisBuild ++= Seq(
      awesomepom, sgodbillon, iliaz, sonatype, sonatypeS, 
      typesafe, t2v, guice, jgitMaven, christophs),
    scalacOptions := Seq(
      "-deprecation",
      "-unchecked",
      "-feature",
      "-language:_")
  )

  lazy val lila = play.Project("lila", "4",
    settings = buildSettings ++ Seq(
      libraryDependencies := Seq(
        scalaz, scalalib, hasher, config, salat, guava, apache, scalaTime,
        paginator, paginatorSalat, csv, jgit, actuarius, scalastic, findbugs,
        reactivemongo),
      templatesImport ++= Seq(
        "lila.app.game.{ DbGame, DbPlayer, Pov }",
        "lila.app.user.User",
        "lila.app.security.Permission",
        "lila.app.templating.Environment._",
        "lila.app.ui",
        "lila.app.http.Context",
        "com.github.ornicar.paginator.Paginator")
    )) dependsOn (user) aggregate (scalachess, common, db, user)

  lazy val common = project("common").settings(
    libraryDependencies := Seq(scalaz, scalalib, jodaTime, jodaConvert, playProvided, guava, 
      reactivemongo)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies := Seq(scalaz, scalalib, playProvided, guava, salat, reactivemongo,
      paginator, paginatorSalat, playReactivemongo)
  )

  lazy val user = project("user", Seq(scalachess, common, db)).settings(
    libraryDependencies := Seq(scalaz, scalalib, jodaTime, jodaConvert, playProvided, salat,
      paginator, paginatorSalat)
  ) 

  lazy val scalachess = project("scalachess").settings(
    libraryDependencies := Seq(scalaz, scalalib, hasher, jodaTime, jodaConvert)
  )

  private type DepType = sbt.ClasspathDep[sbt.ProjectReference]

  private def project(name: String, deps: Seq[DepType] = Seq.empty) = 
    Project(name, file(name), settings = buildSettings, dependencies = deps)
}
