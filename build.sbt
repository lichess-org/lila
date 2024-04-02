import com.typesafe.sbt.packager.Keys.{ bashScriptExtraDefines, scriptClasspath }
import play.sbt.PlayCommands
import play.sbt.PlayInternalKeys.playDependencyClasspath
import play.sbt.routes.RoutesKeys

import BuildSettings.*
import Dependencies.*

lazy val root = Project("lila", file("."))
  .enablePlugins(JavaServerAppPackaging, RoutesCompiler)
  .dependsOn(api)
  .aggregate(api)
  .settings(buildSettings)
  .settings(scalacOptions ++= Seq("-unchecked", "-deprecation"))

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

// cats-parse v1.0.0 is the same as v0.3.1, so this is safe
ThisBuild / libraryDependencySchemes ++= Seq(
  "org.typelevel" %% "cats-parse" % VersionScheme.Always
)

// format: off
libraryDependencies ++= akka.bundle ++ playWs.bundle ++ macwire.bundle ++ scalalib.bundle ++ chess.bundle ++ Seq(
  play.json, play.server, play.netty, play.logback, compression, hasher,
  reactivemongo.driver, /* reactivemongo.kamon, */ maxmind, scalatags,
  kamon.core, kamon.influxdb, kamon.metrics, kamon.prometheus,
  scaffeine, caffeine, lettuce, uaparser, nettyTransport, reactivemongo.shaded
) ++ tests.bundle

lazy val modules = Seq(
  core, common, i18n, db, rating, user, security, socket,
  msg, notifyModule, game, bookmark, search,
  gameSearch, timeline, forum, forumSearch, team, teamSearch,
  analyse, mod, round, pool, lobby, setup,
  importer, gathering, tournament, simul, relation, report, pref,
  evaluation, chat, puzzle, tv, coordinate, feed,
  history, video, shutup, push, appeal, mailer,
  playban, insight, perfStat, irc, challenge,
  study, studySearch, fishnet, explorer, learn, plan,
  event, coach, practice, evalCache, irwin,
  activity, relay, streamer, bot, clas, swiss, storm, racer,
  ublog, tutor, opening, cms, fide, tree
)

lazy val moduleRefs = modules map projectToRef
lazy val moduleCPDeps = moduleRefs map { sbt.ClasspathDependency(_, None) }

lazy val core = module("core",
  Seq(),
  Seq(galimatias, scalatags) ++ scalalib.bundle ++ reactivemongo.bundle ++ tests.bundle
)

lazy val common = module("common",
  Seq(core),
  Seq(
    kamon.core, scaffeine, apacheText, chess.playJson
  ) ++ tests.bundle ++ reactivemongo.bundle ++ flexmark.bundle
)

lazy val db = module("db",
  Seq(common),
  Seq(hasher) ++ macwire.bundle ++ reactivemongo.bundle
)

lazy val memo = module("memo",
  Seq(db),
  Seq(scaffeine) ++ reactivemongo.bundle ++ playWs.bundle
)

lazy val i18n = module("i18n",
  Seq(common),
  tests.bundle ++ Seq(scalatags)
).settings(
  Compile / resourceGenerators += Def.task {
    val outputFile = (Compile / resourceManaged).value / "I18n.ser"
    I18n.serialize(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = "site arena emails learn activity coordinates study class contact appeal patron coach broadcast streamer tfa settings preferences team perfStat search tourname faq lag swiss puzzle puzzleTheme challenge storm ublog insight keyboardMove timeago oauthScope dgt voiceCommands onboarding".split(' ').toList,
      outputFile
    )
  }.taskValue
)

lazy val cms = module("cms",
  Seq(user),
  reactivemongo.bundle
)

lazy val puzzle = module("puzzle",
  Seq(game, user),
  reactivemongo.bundle ++ tests.bundle
)

lazy val storm = module("storm",
  Seq(puzzle),
  reactivemongo.bundle
)

lazy val racer = module("racer",
  Seq(storm, room),
  reactivemongo.bundle
)

lazy val video = module("video",
  Seq(user),
  reactivemongo.bundle ++ macwire.bundle
)

lazy val coach = module("coach",
  Seq(notifyModule, security),
  reactivemongo.bundle
)

lazy val streamer = module("streamer",
  Seq(notifyModule, pref),
  reactivemongo.bundle
)

lazy val coordinate = module("coordinate",
  Seq(user),
  reactivemongo.bundle ++ macwire.bundle
)

lazy val feed = module("feed",
  Seq(user),
  reactivemongo.bundle
)

lazy val ublog = module("ublog",
  Seq(irc, security),
  Seq(bloomFilter) ++ reactivemongo.bundle
)

lazy val evaluation = module("evaluation",
  Seq(analyse),
  tests.bundle ++ reactivemongo.bundle
)

lazy val rating = module("rating",
  Seq(db),
  reactivemongo.bundle ++ tests.bundle ++ Seq(apacheMath)
).dependsOn(common % "test->test")

lazy val perfStat = module("perfStat",
  Seq(game, security),
  reactivemongo.bundle
)

lazy val history = module("history",
  Seq(game),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val search = module("search",
  Seq(common),
  playWs.bundle
)

lazy val chat = module("chat",
  Seq(security),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val room = module("room",
  Seq(chat),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val timeline = module("timeline",
  Seq(relation, security),
  reactivemongo.bundle
)

lazy val event = module("event",
  Seq(user),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val mod = module("mod",
  Seq(evaluation, perfStat, report, history, notifyModule),
  reactivemongo.bundle
)

lazy val user = module("user",
  Seq(rating, memo),
  Seq(hasher, galimatias) ++ tests.bundle ++ playWs.bundle ++ reactivemongo.bundle
)

lazy val game = module("game",
  Seq(tree, user),
  Seq(compression) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val gameSearch = module("gameSearch",
  Seq(game, search),
  reactivemongo.bundle
)

lazy val tv = module("tv",
  Seq(round),
  Seq(hasher) ++ reactivemongo.bundle
)

lazy val bot = module("bot",
  Seq(lobby),
  reactivemongo.bundle
)

lazy val analyse = module("analyse",
  Seq(game),
  tests.bundle ++ reactivemongo.bundle
)

lazy val round = module("round",
  Seq(history, room, fishnet, playban, notifyModule, pref),
  Seq(scalatags, hasher, kamon.core, lettuce) ++ reactivemongo.bundle ++ tests.bundle
)

lazy val pool = module("pool",
  Seq(playban),
  reactivemongo.bundle
)

lazy val activity = module("activity",
  Seq(puzzle),
  reactivemongo.bundle
)

lazy val lobby = module("lobby",
  Seq(game, relation, playban),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val setup = module("setup",
  Seq(lobby),
  reactivemongo.bundle
)

lazy val importer = module("importer",
  Seq(game),
  tests.bundle ++ reactivemongo.bundle
)

lazy val insight = module("insight",
  Seq(analyse, security, pref),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val tutor = module("tutor",
  Seq(insight, fishnet),
  tests.bundle ++ reactivemongo.bundle
)

lazy val opening = module("opening",
  Seq(game),
  tests.bundle
)

lazy val gathering = module("gathering",
  Seq(history),
  tests.bundle
)

lazy val tournament = module("tournament",
  Seq(gathering, round),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val swiss = module("swiss",
  Seq(gathering, round),
  Seq(scalatags, lettuce) ++ reactivemongo.bundle ++ tests.bundle
)

lazy val simul = module("simul",
  Seq(gathering, round),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val fishnet = module("fishnet",
  Seq(analyse),
  Seq(lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val irwin = module("irwin",
  Seq(insight, notifyModule, report),
  reactivemongo.bundle
)

lazy val oauth = module("oauth",
  Seq(user),
  reactivemongo.bundle
)

lazy val security = module("security",
  Seq(irc, oauth, mailer),
  Seq(maxmind, hasher, uaparser) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val shutup = module("shutup",
  Seq(user),
  tests.bundle ++ reactivemongo.bundle
)

lazy val challenge = module("challenge",
  Seq(lobby, room),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val fide = module("fide",
  Seq(memo),
  reactivemongo.bundle
)

lazy val study = module("study",
  Seq(explorer, analyse, notifyModule, room, pref),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle ++ Seq(scalacheck, munitCheck, chess.testKit)
).dependsOn(common % "test->test")

lazy val relay = module("relay",
  Seq(study, round),
  tests.bundle ++ Seq(galimatias) ++ reactivemongo.bundle
)

lazy val studySearch = module("studySearch",
  Seq(study, search),
  reactivemongo.bundle
)

lazy val learn = module("learn",
  Seq(user),
  reactivemongo.bundle
)

lazy val evalCache = module("evalCache",
  Seq(security, tree),
  reactivemongo.bundle
)

lazy val practice = module("practice",
  Seq(study),
  reactivemongo.bundle
)

lazy val playban = module("playban",
  Seq(security, game, chat),
  reactivemongo.bundle
)

lazy val push = module("push",
  Seq(lobby, round),
  Seq(googleOAuth) ++ reactivemongo.bundle
)

lazy val irc = module("irc",
  Seq(user),
  reactivemongo.bundle
)

lazy val mailer = module("mailer",
  Seq(user),
  reactivemongo.bundle ++ Seq(scalatags, hasher, play.mailer)
)

lazy val plan = module("plan",
  Seq(security),
  tests.bundle ++ reactivemongo.bundle
)

lazy val relation = module("relation",
  Seq(game, pref),
  reactivemongo.bundle
)

lazy val pref = module("pref",
  Seq(user),
  reactivemongo.bundle
)

lazy val msg = module("msg",
  Seq(shutup, notifyModule, security, pref),
  reactivemongo.bundle
)

lazy val forum = module("forum",
  Seq(security, pref, notifyModule, pref),
  reactivemongo.bundle
)

lazy val forumSearch = module("forumSearch",
  Seq(search),
  reactivemongo.bundle
)

lazy val team = module("team",
  Seq(chat, security, notifyModule, room),
  reactivemongo.bundle
)

lazy val teamSearch = module("teamSearch",
  Seq(search),
  reactivemongo.bundle
)

lazy val clas = module("clas",
  Seq(msg, puzzle),
  reactivemongo.bundle ++ Seq(bloomFilter)
)

lazy val bookmark = module("bookmark",
  Seq(round),
  reactivemongo.bundle
)

lazy val report = module("report",
  Seq(playban),
  reactivemongo.bundle
)

lazy val appeal = module("appeal",
  Seq(user),
  reactivemongo.bundle
)

lazy val explorer = module("explorer",
  Seq(importer),
  reactivemongo.bundle
)

lazy val notifyModule = module("notify",
  Seq(user, game),
  reactivemongo.bundle
)

lazy val socket = module("socket",
  Seq(memo),
  Seq(lettuce)
)

lazy val tree = module("tree",
  Seq(common),
  Seq(chess.playJson)
)

lazy val api = module("api",
  moduleCPDeps,
  Seq(play.api, play.json, hasher, kamon.core, kamon.influxdb, lettuce) ++ reactivemongo.bundle ++ tests.bundle ++ flexmark.bundle
).settings(
  Runtime / aggregate := false,
  Test / aggregate := true  // Test <: Runtime
) aggregate (moduleRefs: _*)
