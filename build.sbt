import com.typesafe.sbt.packager.Keys.{ bashScriptExtraDefines, scriptClasspath }
import play.sbt.PlayCommands
import play.sbt.PlayInternalKeys.playDependencyClasspath
import play.sbt.routes.RoutesKeys

import BuildSettings._
import Dependencies._

lazy val root = Project("lila", file("."))
  .enablePlugins(JavaServerAppPackaging, RoutesCompiler)
  .dependsOn(api)
  .aggregate(api)
  .settings(buildSettings)
  .settings(scalacOptions ++= Seq("-deprecation"))

organization         := "org.lichess"
Compile / run / fork := true
javaOptions ++= Seq("-Xms64m", "-Xmx512m", "-Dlogger.file=conf/logger.dev.xml")
// shorter prod classpath
scriptClasspath             := Seq("*")
Compile / resourceDirectory := baseDirectory.value / "conf"
// the following settings come from the PlayScala plugin, which I removed
shellPrompt := PlayCommands.playPrompt
// all dependencies from outside the project (all dependency jars)
playDependencyClasspath := (Runtime / externalDependencyClasspath).value
// playCommonClassloader   := PlayCommands.playCommonClassloaderTask.value
// playCompileEverything := PlayCommands.playCompileEverythingTask.value.asInstanceOf[Seq[Analysis]]
ivyLoggingLevel     := UpdateLogging.DownloadOnly
Compile / mainClass := Some("lila.app.Lila")
// Adds the Play application directory to the command line args passed to Play
bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n"
// by default, compile any routes files in the root named "routes" or "*.routes"
Compile / RoutesKeys.routes / sources ++= {
  val dirs = (Compile / unmanagedResourceDirectories).value
  (dirs * "routes").get ++ (dirs * "*.routes").get
}
target                      := baseDirectory.value / "target"
Compile / sourceDirectory   := baseDirectory.value / "app"
Test / sourceDirectory      := baseDirectory.value / "test"
Compile / scalaSource       := baseDirectory.value / "app"
Test / scalaSource          := baseDirectory.value / "test"
Universal / sourceDirectory := baseDirectory.value / "dist"

// format: off
libraryDependencies ++= akka.bundle ++ playWs.bundle ++ macwire.bundle ++ Seq(
  play.json, play.server, play.netty, play.logback,
  chess, compression, scalalib, hasher,
  reactivemongo.driver, /* reactivemongo.kamon, */ maxmind, prismic, scalatags,
  kamon.core, kamon.influxdb, kamon.metrics, kamon.prometheus,
  scaffeine, caffeine, lettuce, uaparser, nettyTransport
) ++ {
  if (shadedMongo) Seq(reactivemongo.shaded)
  else Seq.empty // until reactivemongo includes aarch_64 kqueue versions
} ++ tests.bundle

lazy val modules = Seq(
  common, db, rating, user, security, hub, socket,
  msg, notifyModule, i18n, game, bookmark, search,
  gameSearch, timeline, forum, forumSearch, team, teamSearch,
  analyse, mod, round, pool, lobby, setup,
  importer, tournament, simul, relation, report, pref,
  evaluation, chat, puzzle, tv, coordinate, blog,
  history, video, shutup, push, appeal, mailer,
  playban, insight, perfStat, irc, quote, challenge,
  study, studySearch, fishnet, explorer, learn, plan,
  event, coach, practice, evalCache, irwin,
  activity, relay, streamer, bot, clas, swiss, storm, racer,
  ublog, tutor, opening
)

lazy val moduleRefs = modules map projectToRef
lazy val moduleCPDeps = moduleRefs map { sbt.ClasspathDependency(_, None) }

lazy val api = module("api",
  moduleCPDeps,
  Seq(play.api, play.json, hasher, kamon.core, kamon.influxdb, lettuce) ++ reactivemongo.bundle ++ tests.bundle
).settings(
  Runtime / aggregate := false,
  Test / aggregate := true  // Test <: Runtime
) aggregate (moduleRefs: _*)

lazy val i18n = module("i18n",
  Seq(common, db, hub),
  tests.bundle ++ Seq(scalatags, jodaTime)
).settings(
  Compile / sourceGenerators += Def.task {
    MessageCompiler(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = "site arena emails learn activity coordinates study class contact patron coach broadcast streamer tfa settings preferences team perfStat search tourname faq lag swiss puzzle puzzleTheme challenge storm ublog insight keyboardMove timeago oauthScope".split(' ').toList,
      compileTo = (Compile / sourceManaged).value
    )
  }.taskValue
)

lazy val puzzle = module("puzzle",
  Seq(common, memo, hub, history, db, user, rating, pref, tree, game),
  reactivemongo.bundle ++ tests.bundle
)

lazy val storm = module("storm",
  Seq(common, memo, hub, puzzle, db, user, pref, tree),
  reactivemongo.bundle
)

lazy val racer = module("racer",
  Seq(common, memo, hub, puzzle, storm, db, user, pref, tree, room),
  reactivemongo.bundle
)

lazy val quote = module("quote",
  Seq(),
  Seq(play.json)
)

lazy val video = module("video",
  Seq(common, memo, hub, db, user),
  reactivemongo.bundle ++ macwire.bundle
)

lazy val coach = module("coach",
  Seq(common, hub, db, user, security, notifyModule),
  reactivemongo.bundle
)

lazy val streamer = module("streamer",
  Seq(common, hub, db, user, notifyModule),
  reactivemongo.bundle
)

lazy val coordinate = module("coordinate",
  Seq(common, db, user),
  reactivemongo.bundle ++ macwire.bundle
)

lazy val blog = module("blog",
  Seq(common, memo, timeline),
  Seq(prismic) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val ublog = module("ublog",
  Seq(common, memo, timeline, irc),
  Seq(bloomFilter) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val evaluation = module("evaluation",
  Seq(common, hub, db, user, game, analyse),
  tests.bundle ++ reactivemongo.bundle
)

lazy val common = module("common",
  Seq(),
  Seq(
    scalalib, galimatias, chess,
    kamon.core, scalatags, scaffeine, apacheText
  ) ++ tests.bundle ++ reactivemongo.bundle ++ flexmark.bundle
)

lazy val rating = module("rating",
  Seq(common, db, memo, i18n),
  reactivemongo.bundle ++ tests.bundle
).dependsOn(common % "test->test")

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
  Seq(hasher) ++ macwire.bundle ++ reactivemongo.bundle
)

lazy val memo = module("memo",
  Seq(common, db),
  Seq(scaffeine) ++ reactivemongo.bundle ++ playWs.bundle
)

lazy val search = module("search",
  Seq(common, hub),
  playWs.bundle
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
  Seq(common, db, game, user, hub, security, relation, team),
  reactivemongo.bundle
)

lazy val event = module("event",
  Seq(common, db, memo, i18n, user),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val mod = module("mod",
  Seq(common, db, user, hub, security, tournament, swiss, game, analyse, evaluation, report, notifyModule, history, perfStat, irc),
  reactivemongo.bundle
)

lazy val user = module("user",
  Seq(common, memo, db, hub, rating, socket),
  Seq(hasher, galimatias) ++ tests.bundle ++ playWs.bundle ++ reactivemongo.bundle
)

lazy val game = module("game",
  Seq(common, memo, db, hub, user, chat),
  Seq(compression) ++ tests.bundle ++ reactivemongo.bundle
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
  tests.bundle ++ reactivemongo.bundle
)

lazy val round = module("round",
  Seq(common, db, memo, hub, socket, game, user, i18n, fishnet, pref, chat, history, playban, room, irc),
  Seq(scalatags, hasher, kamon.core, lettuce) ++ reactivemongo.bundle
)

lazy val pool = module("pool",
  Seq(common, game, user, playban),
  reactivemongo.bundle
)

lazy val activity = module("activity",
  Seq(common, game, analyse, user, forum, study, pool, puzzle, tournament, simul, swiss, practice, team, ublog),
  reactivemongo.bundle
)

lazy val lobby = module("lobby",
  Seq(common, db, memo, hub, socket, game, user, round, timeline, relation, playban, security, pool),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val setup = module("setup",
  Seq(common, db, memo, hub, socket, game, user, lobby, pref, relation, oauth),
  reactivemongo.bundle
)

lazy val importer = module("importer",
  Seq(common, game, round),
  tests.bundle ++ reactivemongo.bundle
)

lazy val insight = module("insight",
  Seq(common, game, user, analyse, relation, pref, socket, round, security),
  Seq(scalatags, breeze) ++ reactivemongo.bundle
)

lazy val tutor = module("tutor",
  Seq(common, game, user, analyse, round, insight),
  tests.bundle ++ reactivemongo.bundle
)

lazy val opening = module("opening",
  Seq(common, memo, game),
  tests.bundle
)

lazy val tournament = module("tournament",
  Seq(common, hub, socket, game, round, security, chat, memo, quote, history, notifyModule, i18n, room),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val swiss = module("swiss",
  Seq(common, hub, socket, game, round, security, chat, memo, quote, i18n, room),
  Seq(scalatags, lettuce) ++ reactivemongo.bundle
)

lazy val simul = module("simul",
  Seq(common, hub, socket, game, round, chat, memo, quote, room),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val fishnet = module("fishnet",
  Seq(common, game, analyse, db, evalCache),
  Seq(lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val irwin = module("irwin",
  Seq(common, db, user, game, tournament, mod, insight),
  reactivemongo.bundle
)

lazy val oauth = module("oauth",
  Seq(common, db, user),
  reactivemongo.bundle
)

lazy val security = module("security",
  Seq(common, hub, db, user, i18n, irc, oauth, mailer),
  Seq(maxmind, hasher, uaparser) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val shutup = module("shutup",
  Seq(common, db, hub, game, relation),
  tests.bundle ++ reactivemongo.bundle
)

lazy val challenge = module("challenge",
  Seq(common, db, hub, setup, game, relation, pref, socket, room, msg),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val study = module("study",
  Seq(common, db, hub, socket, game, round, importer, notifyModule, relation, evalCache, explorer, i18n, room),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
).dependsOn(common % "test->test")

lazy val relay = module("relay",
  Seq(common, study),
  Seq(galimatias) ++ reactivemongo.bundle
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
  Seq(common, db, user, game, challenge, msg, pref, notifyModule),
  Seq(googleOAuth) ++ reactivemongo.bundle
)

lazy val irc = module("irc",
  Seq(common, hub, user),
  reactivemongo.bundle
)

lazy val mailer = module("mailer",
  Seq(common, user),
  reactivemongo.bundle ++ Seq(scalatags, hasher)
)

lazy val plan = module("plan",
  Seq(common, user, security),
  tests.bundle ++ reactivemongo.bundle
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
  reactivemongo.bundle ++ Seq(bloomFilter)
)

lazy val bookmark = module("bookmark",
  Seq(common, memo, db, hub, user, game, round),
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

lazy val explorer = module("explorer",
  Seq(common, db, game, importer),
  reactivemongo.bundle
)

lazy val notifyModule = module("notify",
  Seq(common, db, game, user, hub, relation, pref),
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
