import sbt._
import Keys._
import play.Project._

import net.virtualvoid.sbt.graph.{ Plugin ⇒ SbtGraphPlugin }

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val lila = play.Project("lila", "4",
    settings = buildSettings ++ Seq(
      libraryDependencies := Seq(
        scalaz, scalalib, hasher, config, apache, scalaTime,
        csv, jgit, actuarius, scalastic, findbugs, reactivemongo,
        playReactivemongo, spray.caching)
    )) dependsOn api aggregate api settings (
      templatesImport ++= Seq(
        "lila.game.{ Game, Player, Pov }",
        "lila.user.{ User, Context }",
        "lila.security.Permission",
        "lila.app.templating.Environment._",
        "lila.common.paginator.Paginator")
    )

  lazy val modules = Seq(
    chess, common, db, user, wiki, hub, socket,
    message, notification, i18n, game, bookmark, search,
    gameSearch, timeline, forum, forumSearch, team, teamSearch,
    ai, analyse, mod, monitor, site, round, lobby, setup,
    importer)

  lazy val moduleRefs = modules map projectToRef
  lazy val moduleCPDeps = moduleRefs map classpathDependency

  lazy val api = project("api", moduleCPDeps)
    .settings(SbtGraphPlugin.graphSettings: _*)
    .settings(
      libraryDependencies := provided(
        playApi, hasher, config, apache, csv, jgit,
        actuarius, scalastic, findbugs, reactivemongo),
      SbtGraphPlugin.filterScalaLibrary := true
    ) aggregate (moduleRefs: _*)

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(playApi, playTest, reactivemongo, csv)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs) ++ provided(playApi)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(playTest, playApi, reactivemongo, playReactivemongo)
  )

  lazy val search = project("search", Seq(common, hub)).settings(
    libraryDependencies ++= provided(playApi, scalastic)
  )

  lazy val timeline = project("timeline", Seq(common, db, game, user, hub)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo)
  )

  lazy val mod = project("mod", Seq(common, db, user, hub, security)).settings(
    libraryDependencies ++= provided(
      playApi, playTest, reactivemongo, playReactivemongo)
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

  lazy val gameSearch = project("gameSearch", Seq(common, hub, chess, search, game)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val analyse = project("analyse", Seq(common, hub, chess, game, user)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val round = project("round", Seq(
    common, db, memo, hub, socket, chess, game, user, security, i18n, ai)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo)
  )

  lazy val lobby = project("lobby", Seq(
    common, db, memo, hub, socket, chess, game, user, round, timeline)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo)
  )

  lazy val setup = project("setup", Seq(
    common, db, memo, hub, socket, chess, game, user, lobby)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo)
  )

  lazy val importer = project("importer", Seq(common, chess, game, round)).settings(
    libraryDependencies ++= provided(playApi, reactivemongo, playReactivemongo)
  )

  lazy val ai = project("ai", Seq(common, hub, chess, game, analyse)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val security = project("security", Seq(common, hub, db, user)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val message = project("message", Seq(common, db, user, hub)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val forum = project("forum", Seq(common, db, user, security, hub)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val forumSearch = project("forumSearch", Seq(common, hub, forum, search)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val team = project("team", Seq(common, db, user, forum, security, hub)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, spray.caching)
  )

  lazy val teamSearch = project("teamSearch", Seq(common, hub, team, search)).settings(
    libraryDependencies ++= provided(
      playApi, reactivemongo, playReactivemongo, scalastic)
  )

  lazy val i18n = project("i18n", Seq(common, db, user, hub)).settings(
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

  lazy val monitor = project("monitor", Seq(common, hub, socket, db)).settings(
    libraryDependencies ++= provided(playApi, reactivemongo, playReactivemongo)
  )

  lazy val site = project("site", Seq(common, socket)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val socket = project("socket", Seq(common, hub, memo)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val hub = project("hub", Seq(common)).settings(
    libraryDependencies ++= provided(playApi)
  )

  lazy val chess = project("chess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
