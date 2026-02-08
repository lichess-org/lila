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

organization := "org.lichess"
Compile / run / fork := true
javaOptions ++= Seq("-Xms64m", "-Xmx512m", "-Dlogger.file=conf/logger.dev.xml")
javaOptions ++= {
  if (sys.props("java.specification.version").toInt > 21)
    Seq("--enable-native-access=ALL-UNNAMED")
  else
    Seq.empty
}
ThisBuild / scalacOptions ++= Seq("-unchecked", "-deprecation")
ThisBuild / usePipelining := false
// shorter prod classpath
scriptClasspath := Seq("*")
Compile / resourceDirectory := baseDirectory.value / "conf"
// the following settings come from the PlayScala plugin, which I removed
shellPrompt := PlayCommands.playPrompt
// all dependencies from outside the project (all dependency jars)
playDependencyClasspath := (Runtime / externalDependencyClasspath).value
// playCommonClassloader   := PlayCommands.playCommonClassloaderTask.value
// playCompileEverything := PlayCommands.playCompileEverythingTask.value.asInstanceOf[Seq[Analysis]]
ivyLoggingLevel := UpdateLogging.DownloadOnly
Compile / mainClass := Some("lila.app.Lila")
// Adds the Play application directory to the command line args passed to Play
bashScriptExtraDefines += "addJava \"-Duser.dir=$(realpath \"$(cd \"${app_home}/..\"; pwd -P)\"  $(is_cygwin && echo \"fix\"))\"\n"
Compile / RoutesKeys.routes / sources ++= {
  val dirs = (Compile / unmanagedResourceDirectories).value
  (dirs * "routes").get ++ (dirs * "*.routes").get
}
Compile / RoutesKeys.generateReverseRouter := false
Compile / RoutesKeys.generateForwardRouter := true
target := baseDirectory.value / "target"
Compile / sourceDirectory := baseDirectory.value / "app"
Compile / scalaSource := baseDirectory.value / "app"
Universal / sourceDirectory := baseDirectory.value / "dist"

// format: off
libraryDependencies ++= akka.bundle ++ playWs.bundle ++ macwire.bundle ++ scalalib.bundle ++ chess.bundle ++ Seq(
  play.json, play.logback, compression, hasher,
  reactivemongo.driver, /* reactivemongo.kamon, */ maxmind, scalatags,
  kamon.core, kamon.influxdb, kamon.metrics,
  scaffeine, caffeine, lettuce, uaparser, nettyTransport, reactivemongo.shaded, catsMtl
) ++ tests.bundle

// influences the compilation order
// matches https://github.com/ornicar/lila-dep-graphs
lazy val modules = Seq(
  // level 1
  core, coreI18n,
  // level 2
  ui, common, tree,
  // level 3
  db, room, search,
  // level 4
  memo, rating,
  // level 5
  game, gathering, study, user, puzzle, analyse,
  report, pref, chat, playban, lobby, mailer, oauth,
  // level 6
  insight, evaluation, storm,
  // level 7
  // everything else is free from deps; do the big ones first
  relay, security, tournament, plan, round,
  swiss, insight, fishnet, tutor, mod, challenge, web,
  team, forum, streamer, simul, activity, msg, ublog,
  notifyModule, clas, perfStat, opening, timeline,
  setup, video, fide, title, push,
  // and then the smaller ones
  pool, lobby, relation, tv, coordinate, feed, history, recap,
  shutup, appeal, irc, explorer, learn, event, coach,
  practice, evalCache, irwin, bot, racer, cms, i18n, jsBot,
  socket, bookmark, studySearch, gameSearch, forumSearch, teamSearch,
)

lazy val moduleRefs = modules map projectToRef
lazy val moduleCPDeps = moduleRefs map { sbt.ClasspathDependency(_, None) }

lazy val core = module("core",
  Seq(),
  Seq(catsMtl, scalatags, galimatias) ++ scalalib.bundle ++ reactivemongo.bundle ++ tests.bundle
)

lazy val coreI18n = module("coreI18n",
  Seq(),
  Seq(scalatags) ++ scalalib.bundle
)

lazy val common = module("common",
  Seq(core),
  Seq(
    kamon.core, scaffeine, apacheText, chess.playJson
  ) ++ flexmark.bundle
)

lazy val db = module("db",
  Seq(common),
  Seq(hasher) ++ macwire.bundle
)

lazy val memo = module("memo",
  Seq(db),
  Seq(scaffeine) ++ playWs.bundle
)

lazy val i18n = module("i18n",
  Seq(common, coreI18n),
  tests.bundle
).settings(
  Compile / resourceGenerators += Def.task {
    I18n.serialize(
      sourceDir = new File("translation/source"),
      destDir = new File("translation/dest"),
      dbs = "activity appeal arena broadcast challenge class coach contact coordinates dgt emails faq features insight keyboardMove lag learn nvui oauthScope onboarding patron perfStat preferences puzzle puzzleTheme recap search settings site streamer storm study swiss team timeago tfa tourname ublog variant video voiceCommands".split(' ').toList,
      outputDir = (Compile / resourceManaged).value
    )
  }.taskValue
)

lazy val rating = module("rating",
  Seq(db, ui),
  tests.bundle ++ Seq(apacheMath)
).dependsOn(common % "test->test")

lazy val cms = module("cms",
  Seq(memo, ui),
  Seq()
)

lazy val puzzle = module("puzzle",
  Seq(tree, memo, rating),
  tests.bundle
)

lazy val storm = module("storm",
  Seq(puzzle),
  Seq()
)

lazy val racer = module("racer",
  Seq(storm, room),
  Seq()
)

lazy val jsBot = module("jsBot",
  Seq(memo, ui),
  Seq()
)

lazy val video = module("video",
  Seq(memo, ui),
  macwire.bundle
)

lazy val coach = module("coach",
  Seq(memo, rating),
  Seq()
)

lazy val streamer = module("streamer",
  Seq(ui, memo),
  Seq()
)

lazy val coordinate = module("coordinate",
  Seq(db, ui),
  macwire.bundle
)

lazy val feed = module("feed",
  Seq(memo, ui),
  Seq()
)

lazy val ublog = module("ublog",
  Seq(search, report),
  Seq(bloomFilter)
)

lazy val evaluation = module("evaluation",
  Seq(analyse, game),
  tests.bundle
)

lazy val perfStat = module("perfStat",
  Seq(memo, rating),
  Seq()
)

lazy val history = module("history",
  Seq(rating, memo),
  Seq()
)

lazy val search = module("search",
  Seq(memo),
  Seq(playWs.ahc, lilaSearch)
)

lazy val chat = module("chat",
  Seq(memo, ui),
  Seq()
)

lazy val room = module("room",
  Seq(common),
  Seq(lettuce)
)

lazy val timeline = module("timeline",
  Seq(memo, ui),
  Seq()
)

lazy val event = module("event",
  Seq(memo, ui),
  Seq()
)

lazy val mod = module("mod",
  Seq(evaluation, report, chat, user),
  Seq()
)

lazy val user = module("user",
  Seq(rating, memo),
  Seq(hasher) ++ tests.bundle ++ playWs.bundle
)

lazy val game = module("game",
  Seq(tree, rating, memo, bookmark),
  Seq(compression) ++ tests.bundle ++ Seq(scalacheck, munitCheck, chess.testKit)
)

lazy val gameSearch = module("gameSearch",
  Seq(search, ui),
  tests.bundle
)

 // good dep to game
lazy val tv = module("tv",
  Seq(game),
  Seq(hasher)
)

lazy val bot = module("bot",
  Seq(chat, game),
  Seq()
)

lazy val analyse = module("analyse",
  Seq(tree, memo, ui),
  tests.bundle
).dependsOn(coreI18n % "test->test")

lazy val round = module("round",
  Seq(room, game, user, playban, pref, chat),
  Seq(hasher, kamon.core, lettuce) ++ tests.bundle
)

lazy val pool = module("pool",
  Seq(rating),
  Seq()
)

lazy val activity = module("activity",
  Seq(puzzle),
  Seq()
)

lazy val lobby = module("lobby",
  Seq(memo, rating),
  Seq(lettuce) ++ tests.bundle
)

lazy val setup = module("setup",
  Seq(lobby),
  Seq()
)

lazy val insight = module("insight",
  Seq(analyse, game),
  Seq()
)

lazy val tutor = module("tutor",
  Seq(insight),
  tests.bundle
)

lazy val opening = module("opening",
  Seq(memo, ui),
  tests.bundle
)

lazy val gathering = module("gathering",
  Seq(rating),
  tests.bundle
)

lazy val tournament = module("tournament",
  Seq(gathering, room, memo),
  Seq(lettuce) ++ tests.bundle
).dependsOn(coreI18n % "test->test")

lazy val swiss = module("swiss",
  Seq(gathering, room, memo),
  Seq(lettuce) ++ tests.bundle
)

lazy val simul = module("simul",
  Seq(gathering, room, memo),
  Seq(lettuce)
)

lazy val fishnet = module("fishnet",
  Seq(analyse),
  Seq(lettuce) ++ tests.bundle
)

lazy val irwin = module("irwin",
  Seq(analyse, game, report),
  Seq()
)

lazy val oauth = module("oauth",
  Seq(memo, ui),
  Seq()
)

lazy val security = module("security",
  Seq(oauth, user, mailer),
  Seq(maxmind, hasher, uaparser) ++ tests.bundle
)

lazy val shutup = module("shutup",
  Seq(db),
  tests.bundle
)

lazy val challenge = module("challenge",
  Seq(game, room, oauth),
  Seq(lettuce, catsMtl) ++ tests.bundle
)

lazy val fide = module("fide",
  Seq(memo, ui),
  Seq()
)

lazy val title = module("title",
  Seq(memo, ui),
  Seq()
)

lazy val study = module("study",
  Seq(tree, memo, room, ui),
  Seq(lettuce) ++ tests.bundle ++ Seq(scalacheck, munitCheck, chess.testKit)
).dependsOn(common % "test->test")

lazy val relay = module("relay",
  Seq(study, game, report),
  Seq(chess.tiebreak) ++ tests.bundle
).dependsOn(coreI18n % "test->test")

lazy val studySearch = module("studySearch",
  Seq(study, search),
  Seq()
)

lazy val learn = module("learn",
  Seq(db),
  Seq()
)

lazy val evalCache = module("evalCache",
  Seq(tree, memo),
  Seq()
)

lazy val practice = module("practice",
  Seq(study),
  Seq()
)

lazy val playban = module("playban",
  Seq(memo),
  tests.bundle
)

lazy val push = module("push",
  Seq(db),
  playWs.bundle ++ Seq(googleOAuth)
)

lazy val irc = module("irc",
  Seq(common),
  playWs.bundle
)

lazy val mailer = module("mailer",
  Seq(memo, coreI18n),
  Seq(hasher, play.mailer)
)

lazy val plan = module("plan",
  Seq(memo, ui),
  tests.bundle
)

lazy val relation = module("relation",
  Seq(memo, ui),
  Seq()
)

lazy val pref = module("pref",
  Seq(memo, ui),
  Seq()
)

lazy val msg = module("msg",
  Seq(coreI18n, memo),
  Seq()
)

lazy val forum = module("forum",
  Seq(memo, ui, report),
  Seq()
)

lazy val forumSearch = module("forumSearch",
  Seq(search),
  Seq()
)

lazy val team = module("team",
  Seq(memo, room, ui, report),
  Seq()
)

lazy val teamSearch = module("teamSearch",
  Seq(search),
  Seq()
)

lazy val clas = module("clas",
  Seq(user, puzzle),
  Seq(bloomFilter)
)

lazy val bookmark = module("bookmark",
  Seq(db),
  Seq()
)

lazy val report = module("report",
  Seq(ui, memo),
  Seq()
)

lazy val appeal = module("appeal",
  Seq(memo, ui),
  Seq()
)

lazy val explorer = module("explorer",
  Seq(game),
  Seq()
)

lazy val notifyModule = module("notify",
  Seq(memo, ui),
  Seq()
)

lazy val recap = module("recap",
  Seq(user, game, puzzle),
  Seq()
)

lazy val socket = module("socket",
  Seq(memo),
  Seq(lettuce)
)

lazy val tree = module("tree",
  Seq(core),
  Seq(chess.playJson)
)

lazy val ui = module("ui",
  Seq(core, coreI18n),
  Seq()
).enablePlugins(RoutesCompiler).settings(
  Compile / RoutesKeys.generateForwardRouter := false,
  Compile / RoutesKeys.routes / sources ++= {
    val dirs = baseDirectory.value / ".." / ".." / "conf"
    (dirs * "routes").get ++ (dirs * "*.routes").get
  }
)

lazy val web = module("web",
  Seq(ui, memo),
  playWs.bundle ++ tests.bundle ++ Seq(
    play.logback, play.server, play.netty,
    kamon.prometheus,
  )
)

lazy val api = module("api",
  moduleCPDeps,
  Seq(play.api, play.json, hasher, kamon.core, kamon.influxdb, lettuce) ++ tests.bundle ++ flexmark.bundle
).settings(
  Runtime / aggregate := false,
  Test / aggregate := true  // Test <: Runtime
) aggregate (moduleRefs: _*)
