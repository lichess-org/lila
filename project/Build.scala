import sbt._
import Keys._
import play.Project._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val lila = play.Project("lila", "4",
    settings = buildSettings ++ Seq(
      libraryDependencies := Seq(
        scalaz, scalalib, hasher, config, apache, scalaTime,
        csv, jgit, actuarius, scalastic, findbugs,
        spray.caching, reactivemongo)
    )) dependsOn (
      api, user, wiki, message, notification, i18n, game, bookmark,
      gameSearch
    ) aggregate (
        chess, api, common, http, db, user, wiki,
        hub, websocket, message, notification, i18n, game,
        bookmark, search, gameSearch
      ) settings (
          templatesImport ++= Seq(
            "lila.game.{ Game, Player, Pov }",
            "lila.user.{ User, Context }",
            "lila.security.Permission",
            "lila.app.templating.Environment._",
            // "lila.app.ui",
            "lila.common.paginator.Paginator")
        )

  lazy val api = project("api", Seq(common, db, user, security, wiki)).settings(
    libraryDependencies := provided(
      playApi, hasher, config, apache, csv, jgit,
      actuarius, scalastic, findbugs, reactivemongo)
  )

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(playApi, playTest, reactivemongo, csv)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs) ++ provided(playApi)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(playTest, playApi, reactivemongo, playReactivemongo)
  )

  lazy val search = project("search", Seq(common)).settings(
    libraryDependencies ++= provided(playApi, scalastic)
  )

  lazy val gameSearch = project("gameSearch", Seq(common, chess, search, game)).settings(
    libraryDependencies ++= provided(playApi, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val user = project("user", Seq(common, memo, db, chess)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo,
      hasher, spray.caching)
  )

  lazy val game = project("game", Seq(common, db, hub, user, chess)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val http = project("http", Seq(common)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val security = project("security", Seq(common, db, http, user)).settings(
    libraryDependencies ++= provided(playApi, reactivemongo, playReactivemongo)
  )

  lazy val message = project("message", Seq(common, db, user, hub)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val i18n = project("i18n", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, jgit)
  )

  lazy val bookmark = project("bookmark", Seq(common, db, user, game)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val wiki = project("wiki", Seq(common, db)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, jgit, actuarius, guava)
  )

  lazy val notification = project("notification", Seq(common, user, hub)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val websocket = project("websocket", Seq(common, hub, memo)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val hub = project("hub", Seq(common)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val chess = project("scalachess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
