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
  scaffeine, caffeine, lettuce, uaparser, nettyTransport, reactivemongo.shaded
) ++ tests.bundle

lazy val modules = Seq(
  common, db, rating, user, security, hub, socket,
  msg, notifyModule, i18n, game, bookmark, search,
  gameSearch, timeline, forum, forumSearch, team, teamSearch,
  analyse, mod, round, pool, lobby, setup,
  importer, gathering, tournament, simul, relation, report, pref,
  evaluation, chat, puzzle, tv, coordinate, blog,
  history, video, shutup, push, appeal, mailer,
  playban, insight, perfStat, irc, challenge,
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
  Seq(db, hub),
  tests.bundle ++ Seq(scalatags)
).settings(
  Compile / sourceGenerators += Def.task {
    MessageCompiler(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = "site arena emails learn activity coordinates study class contact patron coach broadcast streamer tfa settings preferences team perfStat search tourname faq lag swiss puzzle puzzleTheme challenge storm ublog insight keyboardMove timeago oauthScope dgt".split(' ').toList,
      compileTo = (Compile / sourceManaged).value
    )
  }.taskValue
)

lazy val puzzle = module("puzzle",
  Seq(history, pref),
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
  Seq(notifyModule),
  reactivemongo.bundle
)

lazy val streamer = module("streamer",
  Seq(notifyModule),
  reactivemongo.bundle
)

lazy val coordinate = module("coordinate",
  Seq(user),
  reactivemongo.bundle ++ macwire.bundle
)

lazy val blog = module("blog",
  Seq(timeline),
  Seq(prismic) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val ublog = module("ublog",
  Seq(timeline),
  Seq(bloomFilter) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val evaluation = module("evaluation",
  Seq(analyse),
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
  Seq(i18n, memo),
  reactivemongo.bundle ++ tests.bundle ++ Seq(apacheMath)
).dependsOn(common % "test->test")

lazy val perfStat = module("perfStat",
  Seq(game),
  reactivemongo.bundle
)

lazy val history = module("history",
  Seq(game),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val db = module("db",
  Seq(common),
  Seq(hasher) ++ macwire.bundle ++ reactivemongo.bundle
)

lazy val memo = module("memo",
  Seq(db),
  Seq(scaffeine) ++ reactivemongo.bundle ++ playWs.bundle
)

lazy val search = module("search",
  Seq(hub),
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
  Seq(team),
  reactivemongo.bundle
)

lazy val event = module("event",
  Seq(user),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val mod = module("mod",
  Seq(evaluation, perfStat, tournament, swiss, report),
  reactivemongo.bundle
)

lazy val user = module("user",
  Seq(rating, socket),
  Seq(hasher, galimatias) ++ tests.bundle ++ playWs.bundle ++ reactivemongo.bundle
)

lazy val game = module("game",
  Seq(chat),
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
  Seq(challenge),
  reactivemongo.bundle
)

lazy val analyse = module("analyse",
  Seq(notifyModule, evalCache),
  tests.bundle ++ reactivemongo.bundle
)

lazy val round = module("round",
  Seq(history, room, fishnet, playban),
  Seq(scalatags, hasher, kamon.core, lettuce) ++ reactivemongo.bundle
)

lazy val pool = module("pool",
  Seq(playban),
  reactivemongo.bundle
)

lazy val activity = module("activity",
  Seq(pool, puzzle, simul, practice, ublog),
  reactivemongo.bundle
)

lazy val lobby = module("lobby",
  Seq(timeline, pool),
  Seq(lettuce) ++ reactivemongo.bundle
)

lazy val setup = module("setup",
  Seq(lobby),
  reactivemongo.bundle
)

lazy val importer = module("importer",
  Seq(round),
  tests.bundle ++ reactivemongo.bundle
)

lazy val insight = module("insight",
  Seq(round),
  Seq(scalatags) ++ reactivemongo.bundle
)

lazy val tutor = module("tutor",
  Seq(insight),
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
  Seq(mod, insight),
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
  Seq(relation),
  tests.bundle ++ reactivemongo.bundle
)

lazy val challenge = module("challenge",
  Seq(setup),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
)

lazy val study = module("study",
  Seq(explorer),
  Seq(scalatags, lettuce) ++ tests.bundle ++ reactivemongo.bundle
).dependsOn(common % "test->test")

lazy val relay = module("relay",
  Seq(study),
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
  Seq(security),
  reactivemongo.bundle
)

lazy val practice = module("practice",
  Seq(study),
  reactivemongo.bundle
)

lazy val playban = module("playban",
  Seq(msg),
  reactivemongo.bundle
)

lazy val push = module("push",
  Seq(challenge),
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
  Seq(shutup, notifyModule),
  reactivemongo.bundle
)

lazy val forum = module("forum",
  Seq(mod),
  reactivemongo.bundle
)

lazy val forumSearch = module("forumSearch",
  Seq(forum, search),
  reactivemongo.bundle
)

lazy val team = module("team",
  Seq(forum),
  reactivemongo.bundle
)

lazy val teamSearch = module("teamSearch",
  Seq(team, search),
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
  Seq(relation),
  reactivemongo.bundle
)

lazy val tree = module("tree",
  Seq(common),
  Seq()
)

lazy val socket = module("socket",
  Seq(hub, memo, tree),
  Seq(lettuce)
)

lazy val hub = module("hub",
  Seq(common),
  Seq(scaffeine)
)
