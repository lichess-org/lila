import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val lila = play.Project("lila", "4",
    settings = buildSettings ++ Seq(
      libraryDependencies := Seq(
        scalaz, scalalib, hasher, config, salat, apache, scalaTime,
        csv, jgit, actuarius, scalastic, findbugs,
        reactivemongo),
      templatesImport ++= Seq(
        // "lila.app.game.{ DbGame, DbPlayer, Pov }",
        "lila.user.User",
        // "lila.app.security.Permission",
        // "lila.app.templating.Environment._",
        // "lila.app.ui",
        // "lila.app.http.Context",
        "lila.common.paginator.Paginator")
    )) dependsOn (api, user, wiki) aggregate (scalachess, api, common, db, user, wiki)

  lazy val api = project("api", Seq(common, db, user, wiki)).settings(
    libraryDependencies := provided(
      hasher, config, salat, apache, csv, jgit,
      actuarius, scalastic, findbugs, reactivemongo)
  ).settings(srcMain: _*) aggregate (common, db, user, wiki)

  lazy val common = project("common").settings(
    libraryDependencies ++= Seq(playApi, reactivemongo, spray.util)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(playApi) ++ Seq(reactivemongo, playReactivemongo)
  ).settings(srcMain: _*)

  lazy val user = project("user", Seq(common, memo, db, scalachess)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo, hasher, spray.caching) 
  ).settings(srcMain: _*)

  lazy val wiki = project("wiki", Seq(common, db)).settings(
    libraryDependencies ++= Seq(
      playApi, reactivemongo, playReactivemongo, jgit, actuarius, guava)
  ).settings(srcMain: _*)

  lazy val scalachess = project("scalachess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
