import com.typesafe.sbt.packager.Keys.scriptClasspath

import BuildSettings._
import Dependencies._

lazy val root = Project("lila", file("."))
  .enablePlugins(PlayScala, if (useEpoll) PlayNettyServer else PlayAkkaHttpServer)
  .disablePlugins(if (useEpoll) PlayAkkaHttpServer else PlayNettyServer)
  .dependsOn(api)
  .aggregate(api)
  .settings(buildSettings)

PlayKeys.playDefaultPort := 9663
// don't stage the conf dir
PlayKeys.externalizeResources := false
// shorter prod classpath
scriptClasspath := Seq("*")
// give a fake assets dir to make sure they're not packaged
Assets / resourceDirectory := baseDirectory.value / "public-nothanks"
// don't make an assets jar
PlayKeys.generateAssetsJar := false
// who needs JS routes right?
routesGenerator := LilaRoutesGenerator
maintainer := "contact@lishogi.org"

// format: off
libraryDependencies ++= akka.bundle ++ Seq(
  macwire.macros, macwire.util, play.json, jodaForms, ws,
  shogi, scalalib, hasher,
  reactivemongo.driver, maxmind, prismic, scalatags,
  kamon.core, kamon.influxdb, kamon.metrics, kamon.prometheus,
  scrimage, scaffeine, lettuce, uaparser
) ++ {
  if (useEpoll) Seq(epoll, reactivemongo.epoll)
  else Seq.empty
}

lazy val modules = Seq(
  common, db, rating, user, security, hub, socket,
  msg, notifyModule, i18n, game, bookmark, search,
  gameSearch, timeline, forum, forumSearch, team, teamSearch,
  analyse, mod, round, lobby, setup,
  importer, tournament, simul, relation, report, pref,
  evaluation, chat, puzzle, tv, coordinate, blog,
  history, video, shutup, push, appeal,
  playban, perfStat, quote, challenge,
  study, studySearch, fishnet, learn, plan,
  event, coach, practice, evalCache,
  activity, streamer, bot, clas, storm
)

lazy val moduleRefs = modules map projectToRef
lazy val moduleCPDeps = moduleRefs map { new sbt.ClasspathDependency(_, None) }

lazy val api = module("api",
  moduleCPDeps,
  Seq(play.api, play.json, hasher, kamon.core, kamon.influxdb, lettuce, specs2) ++ reactivemongo.bundle
).settings(
  Runtime / aggregate := false,
  Test / aggregate := true  // Test <: Runtime
) aggregate (moduleRefs: _*)

lazy val i18n = module("i18n",
  Seq(common, db, hub),
  Seq(scalatags)
).settings(
  Compile / sourceGenerators += Def.task {
    MessageCompiler(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = "site arena emails learn activity coordinates study class contact patron coach broadcast streamer tfa settings preferences team perfStat search tourname faq lag puzzle puzzleTheme storm insights pieces nvui".split(' ').toList,
      compileTo = (Compile / sourceManaged).value
    )
  }.taskValue
)

lazy val storm = module("storm",
  Seq(common, memo, hub, puzzle, db, user, rating, pref, tree),
  reactivemongo.bundle
)

lazy val compression = module("compression", Seq(), Seq(specs2))

lazy val puzzle = module("puzzle",
  Seq(common, memo, hub, history, db, user, rating, pref, tree, game),
  reactivemongo.bundle
)

lazy val quote = module("quote",
  Seq(),
  Seq()
)

lazy val video = module("video",
  Seq(common, memo, hub, db, user),
  reactivemongo.bundle
)

lazy val coach = module("coach",
  Seq(common, hub, db, user, security, game),
  reactivemongo.bundle
)

lazy val streamer = module("streamer",
  Seq(common, hub, db, user, notifyModule),
  reactivemongo.bundle
)

lazy val coordinate = module("coordinate",
  Seq(common, db, user),
  reactivemongo.bundle
)

lazy val blog = module("blog",
  Seq(common, memo, timeline),
  Seq(prismic, specs2) ++ flexmark.bundle ++ reactivemongo.bundle
)

lazy val evaluation = module("evaluation",
  Seq(common, hub, db, user, game, analyse),
  Seq(specs2) ++ reactivemongo.bundle
)

lazy val common = module("common",
  Seq(),
  Seq(kamon.core, scalatags, jodaForms, scaffeine, specs2) ++ reactivemongo.bundle
)

lazy val rating = module("rating",
  Seq(common, db, memo, i18n),
  reactivemongo.bundle
)

lazy val perfStat = module("perfStat",
  Seq(common, db, user, game, rating),
  reactivemongo.bundle
)

lazy val history = module("history",
  Seq(common, db, memo, game, user),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val db = module("db",
  Seq(common),
  Seq(hasher, scrimage) ++ reactivemongo.bundle
)

lazy val memo = module("memo",
  Seq(common, db),
  Seq(scaffeine, scalatest, akka.testkit) ++ reactivemongo.bundle
)

lazy val search = module("search",
  Seq(common, hub),
  Seq()
)

lazy val chat = module("chat",
  Seq(common, db, user, security, i18n, socket),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val room = module("room",
  Seq(common, socket, chat),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val timeline = module("timeline",
  Seq(common, db, game, user, hub, security, relation),
  reactivemongo.bundle
)

lazy val event = module("event",
  Seq(common, db, memo, i18n, user),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val mod = module("mod",
  Seq(common, db, user, hub, security, tournament, simul, game, analyse, evaluation, report, notifyModule, history, perfStat),
  reactivemongo.bundle
)

lazy val user = module("user",
  Seq(common, memo, db, hub, rating, socket),
  Seq(hasher, specs2) ++ reactivemongo.bundle
)

lazy val game = module("game",
  Seq(common, memo, db, hub, user, chat, compression),
  Seq(specs2) ++ reactivemongo.bundle
)

lazy val gameSearch = module("gameSearch",
  Seq(common, hub, search, game),
  reactivemongo.bundle
)

lazy val tv = module("tv",
  Seq(common, db, hub, socket, game, round, user),
  Seq(hasher) ++ reactivemongo.bundle
)

lazy val bot = module("bot",
  Seq(common, db, hub, game, user, challenge, chat, socket),
  reactivemongo.bundle
)

lazy val analyse = module("analyse",
  Seq(common, hub, game, user, notifyModule, evalCache),
  reactivemongo.bundle
)

lazy val round = module("round",
  Seq(common, db, memo, hub, socket, game, user, i18n, fishnet, pref, chat, history, playban, room),
  Seq(scalatags, hasher, kamon.core, lettuce) ++ reactivemongo.bundle
)

lazy val activity = module("activity",
  Seq(common, game, analyse, user, forum, study, puzzle, tournament, practice, team),
  reactivemongo.bundle
)

lazy val lobby = module("lobby",
  Seq(common, db, memo, hub, socket, game, user, round, timeline, relation, playban, security),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val setup = module("setup",
  Seq(common, db, memo, hub, socket, game, user, lobby, pref, relation),
  reactivemongo.bundle
)

lazy val importer = module("importer",
  Seq(common, game, round),
  reactivemongo.bundle
)

lazy val tournament = module("tournament",
  Seq(common, hub, socket, game, round, security, chat, memo, quote, history, notifyModule, i18n, room),
  Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val simul = module("simul",
  Seq(common, hub, socket, game, round, chat, memo, quote, room),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val fishnet = module("fishnet",
  Seq(common, game, analyse, db, evalCache, puzzle),
  Seq(lettuce, specs2) ++ reactivemongo.bundle
)

lazy val oauth = module("oauth",
  Seq(common, db, user),
  Seq(galimatias) ++ reactivemongo.bundle
)

lazy val security = module("security",
  Seq(common, hub, db, user, i18n, oauth),
  Seq(scalatags, maxmind, hasher, uaparser, specs2) ++ reactivemongo.bundle
)

lazy val shutup = module("shutup",
  Seq(common, db, hub, game, relation),
  Seq(specs2) ++ reactivemongo.bundle
)

lazy val challenge = module("challenge",
  Seq(common, db, hub, setup, game, relation, pref, socket, room),
  Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val study = module("study",
  Seq(common, db, hub, socket, game, round, importer, notifyModule, relation, evalCache, i18n, room),
  Seq(scalatags, lettuce, specs2) ++ reactivemongo.bundle
)

lazy val studySearch = module("studySearch",
  Seq(common, hub, study, search),
  reactivemongo.bundle
)

lazy val learn = module("learn",
  Seq(common, db, user),
  reactivemongo.bundle
)

lazy val evalCache = module("evalCache",
  Seq(common, db, user, security, socket, tree),
  reactivemongo.bundle
)

lazy val practice = module("practice",
  Seq(common, db, memo, user, study),
  reactivemongo.bundle
)

lazy val playban = module("playban",
  Seq(common, db, game, msg, chat),
  reactivemongo.bundle
)

lazy val push = module("push",
  Seq(common, db, user, game, challenge, msg),
  Seq(googleOAuth) ++ reactivemongo.bundle
)

lazy val plan = module("plan",
  Seq(common, user, notifyModule),
  reactivemongo.bundle
)

lazy val relation = module("relation",
  Seq(common, db, memo, hub, user, game, pref),
  reactivemongo.bundle
)

lazy val pref = module("pref",
  Seq(common, db, user),
  reactivemongo.bundle
)

lazy val msg = module("msg",
  Seq(common, db, user, hub, relation, security, shutup, notifyModule, chat),
  reactivemongo.bundle
)

lazy val forum = module("forum",
  Seq(common, db, user, security, hub, mod, notifyModule),
  reactivemongo.bundle
)

lazy val forumSearch = module("forumSearch",
  Seq(common, hub, forum, search),
  reactivemongo.bundle
)

lazy val team = module("team",
  Seq(common, memo, db, user, forum, security, hub, notifyModule),
  reactivemongo.bundle
)

lazy val teamSearch = module("teamSearch",
  Seq(common, hub, team, search),
  reactivemongo.bundle
)

lazy val clas = module("clas",
  Seq(common, memo, db, user, security, msg, history, puzzle),
  flexmark.bundle ++ reactivemongo.bundle
)

lazy val bookmark = module("bookmark",
  Seq(common, memo, db, hub, user, game),
  reactivemongo.bundle
)

lazy val report = module("report",
  Seq(common, db, user, game, security, playban),
  reactivemongo.bundle
)

lazy val appeal = module("appeal",
  Seq(common, db, user),
  reactivemongo.bundle
)

lazy val notifyModule = module("notify",
  Seq(common, db, game, user, hub, relation),
  reactivemongo.bundle
)

lazy val tree = module("tree",
  Seq(common),
  Seq()
)

lazy val socket = module("socket",
  Seq(common, hub, memo, tree),
  Seq(lettuce)
)

lazy val hub = module("hub",
  Seq(common),
  Seq(scaffeine)
)
