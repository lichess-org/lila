import play.Project._
import sbt._, Keys._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  override def rootProject = Some(lila)

  lazy val lila = _root_.play.Project("lila", "5.0") settings (
    offline := true,
    libraryDependencies ++= Seq(
      scalaz, scalalib, hasher, config, apache, scalaTime,
      csv, jgit, actuarius, elastic4s, findbugs, RM,
      PRM, spray.caching, maxmind),
      scalacOptions := compilerOptions,
      sources in doc in Compile := List(),
      incOptions := incOptions.value.withNameHashing(true),
      templatesImport ++= Seq(
        "lila.game.{ Game, Player, Pov }",
        "lila.user.{ User, UserContext }",
        "lila.security.Permission",
        "lila.app.templating.Environment._",
        "lila.api.Context",
        "lila.common.paginator.Paginator")
  ) dependsOn api aggregate api

  lazy val modules = Seq(
    chess, common, db, rating, user, security, wiki, hub, socket,
    message, notification, i18n, game, bookmark, search,
    gameSearch, timeline, forum, forumSearch, team, teamSearch,
    ai, analyse, mod, monitor, site, round, lobby, setup,
    importer, tournament, relation, report, pref, simulation,
    evaluation, chat, puzzle, tv)

  lazy val moduleRefs = modules map projectToRef
  lazy val moduleCPDeps = moduleRefs map { new sbt.ClasspathDependency(_, None) }

  lazy val api = project("api", moduleCPDeps)
    .settings(
      libraryDependencies ++= provided(
        play.api, hasher, config, apache, csv, jgit,
        actuarius, elastic4s, findbugs, RM)
    ) aggregate (moduleRefs: _*)

  lazy val puzzle = project("puzzle", Seq(
    common, memo, hub, db, user, rating)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val evaluation = project("evaluation", Seq(
    common, hub, db, user, game)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val simulation = project("simulation", Seq(
    common, hub, socket, game, tv, round, setup)).settings(
    libraryDependencies ++= provided(play.api, RM)
  )

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(play.api, play.test, RM, csv)
  )

  lazy val rating = project("rating", Seq(common, db)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val memo = project("memo", Seq(common)).settings(
    libraryDependencies ++= Seq(guava, findbugs, spray.caching) ++ provided(play.api)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(play.test, play.api, RM, PRM)
  )

  lazy val search = project("search", Seq(common, hub)).settings(
    libraryDependencies ++= provided(play.api, elastic4s)
  )

  lazy val chat = project("chat", Seq(
    common, db, user, security, i18n)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM)
  )

  lazy val timeline = project("timeline", Seq(common, db, game, user, hub, security, relation)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, RM, PRM)
  )

  lazy val mod = project("mod", Seq(common, db, user, hub, security)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, RM, PRM)
  )

  lazy val user = project("user", Seq(common, memo, db, hub, chess, rating)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, RM, PRM, hasher)
  )

  lazy val game = project("game", Seq(common, memo, db, hub, user, chess, chat)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM)
  )

  lazy val gameSearch = project("gameSearch", Seq(common, hub, chess, search, game, analyse)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, elastic4s)
  )

  lazy val tv = project("tv", Seq(common, db, hub, game, chess)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM)
  )

  lazy val analyse = project("analyse", Seq(common, hub, chess, game, user)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, spray.caching)
  )

  lazy val round = project("round", Seq(
    common, db, memo, hub, socket, chess, game, user, i18n, ai, pref, chat)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val lobby = project("lobby", Seq(
    common, db, memo, hub, socket, chess, game, user, round, timeline, relation)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val setup = project("setup", Seq(
    common, db, memo, hub, socket, chess, game, user, lobby)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val importer = project("importer", Seq(common, chess, game, round)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val tournament = project("tournament", Seq(
    common, hub, socket, chess, game, round, setup, security, chat, memo)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val ai = project("ai", Seq(common, hub, chess, game, analyse, rating)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val security = project("security", Seq(common, hub, db, user)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, maxmind)
  )

  lazy val relation = project("relation", Seq(common, db, memo, hub, user, game, pref)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val pref = project("pref", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val message = project("message", Seq(common, db, user, hub, relation, security)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, spray.caching)
  )

  lazy val forum = project("forum", Seq(common, db, user, security, hub, mod)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, spray.caching)
  )

  lazy val forumSearch = project("forumSearch", Seq(common, hub, forum, search)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, elastic4s)
  )

  lazy val team = project("team", Seq(common, memo, db, user, forum, security, hub)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM)
  )

  lazy val teamSearch = project("teamSearch", Seq(common, hub, team, search)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, elastic4s)
  )

  lazy val i18n = project("i18n", Seq(common, db, user, hub)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, jgit)
  )

  lazy val bookmark = project("bookmark", Seq(common, memo, db, hub, user, game)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, RM, PRM)
  )

  lazy val wiki = project("wiki", Seq(common, db)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM, jgit, actuarius, guava)
  )

  lazy val report = project("report", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(
      play.api, RM, PRM)
  )

  lazy val notification = project("notification", Seq(common, user, hub)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val monitor = project("monitor", Seq(common, hub, socket, db)).settings(
    libraryDependencies ++= provided(play.api, RM, PRM)
  )

  lazy val site = project("site", Seq(common, socket)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val socket = project("socket", Seq(common, hub, memo)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val hub = project("hub", Seq(common)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val chess = project("chess").settings(
    libraryDependencies ++= Seq(hasher)
  )
}
