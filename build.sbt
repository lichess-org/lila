import com.typesafe.sbt.packager.Keys.scriptClasspath
import com.typesafe.sbt.SbtScalariform.autoImport.scalariformFormat

import BuildSettings._
import Dependencies._

lazy val root = Project("lila", file("."))
  .enablePlugins(PlayScala, PlayNettyServer)
  .disablePlugins(PlayAkkaHttpServer)
  .dependsOn(api)
  .aggregate(api)

scalaVersion := globalScalaVersion
resolvers ++= Dependencies.Resolvers.commons
scalacOptions ++= compilerOptions
sources in doc in Compile := List()
// disable publishing the main API jar
publishArtifact in (Compile, packageDoc) := false
// disable publishing the main sources jar
publishArtifact in (Compile, packageSrc) := false
PlayKeys.playDefaultPort := 9663
// don't stage the conf dir
PlayKeys.externalizeResources := false
// shorter prod classpath
scriptClasspath := Seq("*")
// offline := true
libraryDependencies ++= Seq(
  macwire.macros, macwire.util, play.json, jodaForms, ws,
  scalaz, chess, compression, scalalib, hasher,
  reactivemongo.driver, reactivemongo.bson, reactivemongo.native,
  maxmind, prismic, markdown, scalatags,
  kamon.core, kamon.influxdb, kamon.metrics,
  scrimage, scaffeine, lettuce, epoll
)
resourceDirectory in Assets := (sourceDirectory in Compile).value / "assets"
unmanagedResourceDirectories in Assets ++= (if (scala.sys.env.get("SERVE_ASSETS").exists(_ == "1")) Seq(baseDirectory.value / "public") else Nil)

scalariformPreferences := scalariformPrefs(scalariformPreferences.value)
excludeFilter in scalariformFormat := "*Routes*"

lazy val modules = Seq(
  common, db, rating, user, security, hub, socket,
  message, notifyModule, i18n, game, bookmark, search,
  gameSearch, timeline, forum, forumSearch, team, teamSearch,
  analyse, mod, round, pool, lobby, setup,
  importer, tournament, simul, relation, report, pref,
  evaluation, chat, puzzle, tv, coordinate, blog,
  history, video, shutup, push,
  playban, insight, perfStat, slack, quote, challenge,
  study, studySearch, fishnet, explorer, learn, plan,
  event, coach, practice, evalCache, irwin,
  activity, relay, streamer, bot
)

lazy val moduleRefs = modules map projectToRef
lazy val moduleCPDeps = moduleRefs map { new sbt.ClasspathDependency(_, None) }

lazy val api = module("api",
  moduleCPDeps
).settings(
  libraryDependencies ++= Seq(hasher, kamon.core, lettuce) ++ reactivemongo.bundle,
  aggregate in Runtime := false,
  aggregate in Test := true  // Test <: Runtime
) aggregate (moduleRefs: _*)

lazy val i18n = module("i18n",
  Seq(common, db, user, hub)
).settings(
  libraryDependencies ++= Seq(scalatags),
  sourceGenerators in Compile += Def.task {
    MessageCompiler(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = List("site", "arena", "emails", "learn", "activity", "coordinates", "study"),
      compileTo = (sourceManaged in Compile).value / "messages"
    )
  }.taskValue
)

lazy val puzzle = module("puzzle",
  Seq(common, memo, hub, history, db, user, rating, pref, tree, game)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val quote = module("quote",
  Seq()
).settings(
  libraryDependencies ++= Seq()
)

lazy val video = module("video",
  Seq(common, memo, hub, db, user)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val coach = module("coach",
  Seq(common, hub, db, user, security, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val streamer = module("streamer",
  Seq(common, hub, db, user, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val coordinate = module("coordinate",
  Seq(common, db)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val blog = module("blog",
  Seq(common, memo, timeline)
).settings(
  libraryDependencies ++= Seq(prismic) ++ reactivemongo.bundle
)

lazy val evaluation = module("evaluation",
  Seq(common, hub, db, user, game, analyse)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val common = module("common",
  Seq()
).settings(
  libraryDependencies ++= Seq(kamon.core, scalatags, jodaForms, scaffeine) ++ reactivemongo.bundle
)

lazy val rating = module("rating",
  Seq(common, db, memo)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val perfStat = module("perfStat",
  Seq(common, db, user, game, rating)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val history = module("history",
  Seq(common, db, memo, game, user)
).settings(
  libraryDependencies ++= Seq(scalatags) ++ reactivemongo.bundle
)

lazy val db = module("db",
  Seq(common)
).settings(
  libraryDependencies ++= Seq(hasher, scrimage) ++ reactivemongo.bundle
)

lazy val memo = module("memo",
  Seq(common, db)
).settings(
  libraryDependencies ++= Seq(scaffeine, play.api) ++ reactivemongo.bundle
)

lazy val search = module("search",
  Seq(common, hub)
).settings(
  libraryDependencies ++= Seq()
)

lazy val chat = module("chat",
  Seq(common, db, user, security, i18n, socket)
).settings(
  libraryDependencies ++= Seq(scalatags) ++ reactivemongo.bundle
)

lazy val room = module("room",
  Seq(common, socket, chat)
).settings(
  libraryDependencies ++= Seq(lettuce) ++ reactivemongo.bundle
)

lazy val timeline = module("timeline",
  Seq(common, db, game, user, hub, security, relation)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val event = module("event",
  Seq(common, db, memo, i18n)
).settings(
  libraryDependencies ++= Seq(scalatags) ++ reactivemongo.bundle
)

lazy val mod = module("mod",
  Seq(common, db, user, hub, security, tournament, simul, game, analyse, evaluation, report, notifyModule, history, perfStat)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val user = module("user",
  Seq(common, memo, db, hub, rating, socket)
).settings(
  libraryDependencies ++= Seq(hasher) ++ reactivemongo.bundle
)

lazy val game = module("game",
  Seq(common, memo, db, hub, user, chat)
).settings(
  libraryDependencies ++= Seq(compression, play.api) ++ reactivemongo.bundle
)

lazy val gameSearch = module("gameSearch",
  Seq(common, hub, search, game)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val tv = module("tv",
  Seq(common, db, hub, socket, game, round, user)
).settings(
  libraryDependencies ++= Seq(hasher) ++ reactivemongo.bundle
)

lazy val bot = module("bot",
  Seq(common, db, hub, game, user, challenge, chat)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val analyse = module("analyse",
  Seq(common, hub, game, user, notifyModule, evalCache)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val round = module("round",
  Seq(common, db, memo, hub, socket, game, user, i18n, fishnet, pref, chat, history, playban, room)
).settings(
  libraryDependencies ++= Seq(scalatags, hasher, lettuce) ++ reactivemongo.bundle
)

lazy val pool = module("pool",
  Seq(common, game, user, playban)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val activity = module("activity",
  Seq(common, game, analyse, user, forum, study, pool, puzzle, tournament, practice, team)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val lobby = module("lobby",
  Seq(common, db, memo, hub, socket, game, user, round, timeline, relation, playban, security, pool)
).settings(
  libraryDependencies ++= Seq(lettuce) ++ reactivemongo.bundle
)

lazy val setup = module("setup",
  Seq(common, db, memo, hub, socket, game, user, lobby, pref, relation)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val importer = module("importer",
  Seq(common, game, round)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val insight = module("insight",
  Seq(common, game, user, analyse, relation, pref, socket, round, security)
).settings(
  libraryDependencies ++= Seq(scalatags) ++ reactivemongo.bundle
)

lazy val tournament = module("tournament",
  Seq(common, hub, socket, game, round, security, chat, memo, quote, history, notifyModule, i18n, room)
).settings(
  libraryDependencies ++= Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val simul = module("simul",
  Seq(common, hub, socket, game, round, chat, memo, quote, room)
).settings(
  libraryDependencies ++= Seq(lettuce) ++ reactivemongo.bundle
)

lazy val fishnet = module("fishnet",
  Seq(common, game, analyse, db, evalCache)
).settings(
  libraryDependencies ++= Seq(lettuce) ++ reactivemongo.bundle
)

lazy val irwin = module("irwin",
  Seq(common, db, user, game, tournament, mod)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val oauth = module("oauth",
  Seq(common, db, user)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val security = module("security",
  Seq(common, hub, db, user, i18n, slack, oauth)
).settings(
  libraryDependencies ++= Seq(scalatags, maxmind, hasher) ++ reactivemongo.bundle
)

lazy val shutup = module("shutup",
  Seq(common, db, hub, game, relation)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val challenge = module("challenge",
  Seq(common, db, hub, setup, game, relation, pref, socket, room)
).settings(
  libraryDependencies ++= Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val study = module("study",
  Seq(common, db, hub, socket, game, round, importer, notifyModule, relation, evalCache, explorer, i18n, room)
).settings(
  libraryDependencies ++= Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val relay = module("relay",
  Seq(common, study)
).settings(
  libraryDependencies ++= Seq(scalaUri, markdown) ++ reactivemongo.bundle
)

lazy val studySearch = module("studySearch",
  Seq(common, hub, study, search)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val learn = module("learn",
  Seq(common, db, user)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val evalCache = module("evalCache",
  Seq(common, db, user, security, socket, tree)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val practice = module("practice",
  Seq(common, db, memo, user, study)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val playban = module("playban",
  Seq(common, db, game, message, chat)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val push = module("push",
  Seq(common, db, user, game, challenge, message)
).settings(
  libraryDependencies ++= Seq(googleOAuth, play.api) ++ reactivemongo.bundle
)

lazy val slack = module("slack",
  Seq(common, hub, user)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val plan = module("plan",
  Seq(common, user, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val relation = module("relation",
  Seq(common, db, memo, hub, user, game, pref)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val pref = module("pref",
  Seq(common, db, user)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val message = module("message",
  Seq(common, db, user, hub, relation, security, shutup, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val forum = module("forum",
  Seq(common, db, user, security, hub, mod, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val forumSearch = module("forumSearch",
  Seq(common, hub, forum, search)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val team = module("team",
  Seq(common, memo, db, user, forum, security, hub, notifyModule)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val teamSearch = module("teamSearch",
  Seq(common, hub, team, search)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val bookmark = module("bookmark",
  Seq(common, memo, db, hub, user, game)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val report = module("report",
  Seq(common, db, user, game, security, playban)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val explorer = module("explorer",
  Seq(common, db, game, importer)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val notifyModule = module("notify",
  Seq(common, db, game, user, hub, relation)
).settings(
  libraryDependencies ++= reactivemongo.bundle
)

lazy val tree = module("tree",
  Seq(common)
).settings(
  libraryDependencies ++= Seq()
)

lazy val socket = module("socket",
  Seq(common, hub, memo, tree)
).settings(
  libraryDependencies ++= Seq(lettuce)
)

lazy val hub = module("hub",
  Seq(common)
).settings(
  libraryDependencies ++= Seq(scaffeine, play.api)
)
