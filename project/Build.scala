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
    )) dependsOn (api, user, wiki) aggregate (scalachess, api, common, http, db, user, wiki)

  lazy val api = project("api", Seq(common, db, user, security, wiki)).settings(
    libraryDependencies := provided(
      hasher, config, salat, apache, csv, jgit,
      actuarius, scalastic, findbugs, reactivemongo)
  )

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(playApi, reactivemongo)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs) ++ provided(playApi)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(playApi) ++ Seq(reactivemongo, playReactivemongo)
  )

  lazy val user = project("user", Seq(common, memo, db, scalachess)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo, hasher, spray.caching) 
  )

  lazy val http = project("http", Seq(common, user)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val security = project("security", Seq(common, db, http, user)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo) 
  )

  lazy val wiki = project("wiki", Seq(common, db)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, jgit, actuarius, guava)
  )

  lazy val scalachess = project("scalachess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
