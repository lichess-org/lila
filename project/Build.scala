import com.typesafe.sbt.packager.Keys.scriptClasspath
import com.typesafe.sbt.web.SbtWeb.autoImport._
import play.Play.autoImport._
import play.sbt.PlayImport._
import play.twirl.sbt.Import._
import PlayKeys._
import sbt._, Keys._

object ApplicationBuild extends Build {

  import BuildSettings._
  import Dependencies._

  lazy val root = Project("lila", file("."))
    .enablePlugins(_root_.play.sbt.PlayScala)
    .dependsOn(api)
    .aggregate(api)
    .settings(Seq(
      scalaVersion := globalScalaVersion,
      resolvers ++= Dependencies.Resolvers.commons,
      scalacOptions := compilerOptions,
      incOptions := incOptions.value.withNameHashing(true),
      updateOptions := updateOptions.value.withCachedResolution(true),
      sources in doc in Compile := List(),
      // disable publishing the main API jar
      publishArtifact in (Compile, packageDoc) := false,
      // disable publishing the main sources jar
      publishArtifact in (Compile, packageSrc) := false,
      // don't stage the conf dir
      externalizeResources := false,
      // shorter prod classpath
      scriptClasspath := Seq("*"),
      // offline := true,
      libraryDependencies ++= Seq(
        scalaz, scalalib, hasher, config, apache,
        jgit, findbugs, reactivemongo.driver, reactivemongo.iteratees, akka.actor, akka.slf4j,
        maxmind, prismic,
        kamon.core, kamon.statsd, kamon.influxdb,
        java8compat, semver, scrimage, configs, scaffeine),
      TwirlKeys.templateImports ++= Seq(
        "lila.game.{ Game, Player, Pov }",
        "lila.tournament.Tournament",
        "lila.user.{ User, UserContext }",
        "lila.security.Permission",
        "lila.app.templating.Environment._",
        "lila.api.Context",
        "lila.common.paginator.Paginator"),
      // trump sbt-web into not looking at public/
      resourceDirectory in Assets := (sourceDirectory in Compile).value / "assets"
    ))

  lazy val modules = Seq(
    chess, common, db, rating, user, security, hub, socket,
    message, notifyModule, i18n, game, bookmark, search,
    gameSearch, timeline, forum, forumSearch, team, teamSearch,
    analyse, mod, site, round, pool, lobby, setup,
    importer, tournament, simul, relation, report, pref, // simulation,
    evaluation, chat, puzzle, tv, coordinate, blog, qa,
    history, video, shutup, push,
    playban, insight, perfStat, slack, quote, challenge,
    study, studySearch, fishnet, explorer, learn, plan,
    event, coach, practice, evalCache)

  lazy val moduleRefs = modules map projectToRef
  lazy val moduleCPDeps = moduleRefs map { new sbt.ClasspathDependency(_, None) }

  lazy val api = project("api", moduleCPDeps)
    .settings(
      libraryDependencies ++= provided(
        play.api, hasher, config, apache, jgit, findbugs,
        reactivemongo.driver, reactivemongo.iteratees,
        kamon.core, kamon.statsd, kamon.influxdb)
    ) aggregate (moduleRefs: _*)

  lazy val puzzle = project("puzzle", Seq(
    common, memo, hub, db, user, rating, pref, tree, game)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val quote = project("quote", Seq())

  lazy val video = project("video", Seq(
    common, memo, hub, db, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val coach = project("coach", Seq(
    common, hub, db, user, security, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, scrimage)
  )

  lazy val coordinate = project("coordinate", Seq(common, db)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val qa = project("qa", Seq(common, db, memo, user, security, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val blog = project("blog", Seq(common, memo, user, message)).settings(
    libraryDependencies ++= provided(play.api, prismic,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val evaluation = project("evaluation", Seq(
    common, hub, db, user, game, analyse)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  // lazy val simulation = project("simulation", Seq(
  //   common, hub, socket, game, tv, round, setup)).settings(
  //   libraryDependencies ++= provided(play.api, reactivemongo.driver)
  // )

  lazy val common = project("common").settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo.driver, kamon.core)
  )

  lazy val rating = project("rating", Seq(common, db, chess)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val perfStat = project("perfStat", Seq(common, db, chess, user, game, rating)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val history = project("history", Seq(common, db, memo, game, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val db = project("db", Seq(common)).settings(
    libraryDependencies ++= provided(play.test, play.api, reactivemongo.driver, hasher)
  )

  lazy val memo = project("memo", Seq(common, db)).settings(
    libraryDependencies ++= Seq(findbugs, scaffeine, configs) ++ provided(play.api, reactivemongo.driver)
  )

  lazy val search = project("search", Seq(common, hub)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val chat = project("chat", Seq(common, db, user, security, i18n, socket)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val timeline = project("timeline", Seq(common, db, game, user, hub, security, relation)).settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo.driver)
  )

  lazy val event = project("event", Seq(common, db, memo)).settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo.driver)
  )

  lazy val mod = project("mod", Seq(common, db, user, hub, security, tournament, simul, game, analyse, evaluation,
    report, notifyModule, history)).settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo.driver)
  )

  lazy val user = project("user", Seq(common, memo, db, hub, chess, rating)).settings(
    libraryDependencies ++= provided(play.api, play.test, reactivemongo.driver, hasher)
  )

  lazy val game = project("game", Seq(common, memo, db, hub, user, chess, chat)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val gameSearch = project("gameSearch", Seq(common, hub, chess, search, game)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo.driver, reactivemongo.iteratees))

  lazy val tv = project("tv", Seq(common, db, hub, socket, game, user, chess)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, hasher)
  )

  lazy val analyse = project("analyse", Seq(common, hub, chess, game, user, notifyModule, evalCache)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val round = project("round", Seq(
    common, db, memo, hub, socket, chess, game, user,
    i18n, fishnet, pref, chat, history, playban)).settings(
    libraryDependencies ++= provided(play.api, hasher, kamon.core,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val pool = project("pool", Seq(common, game, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val lobby = project("lobby", Seq(
    common, db, memo, hub, socket, chess, game, user,
    round, timeline, relation, playban, security, pool)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val setup = project("setup", Seq(
    common, db, memo, hub, socket, chess, game, user, lobby, pref, relation)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val importer = project("importer", Seq(common, chess, game, round)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val insight = project(
    "insight",
    Seq(common, chess, game, user, analyse, relation, pref, socket, round, security)
  ).settings(
      libraryDependencies ++= provided(
        play.api,
        reactivemongo.driver, reactivemongo.iteratees)
    )

  lazy val tournament = project("tournament", Seq(
    common, hub, socket, chess, game, round, security, chat, memo, quote, history, notifyModule)).settings(
    libraryDependencies ++= provided(
      play.api,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val simul = project("simul", Seq(
    common, hub, socket, chess, game, round, chat, memo, quote)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val fishnet = project("fishnet", Seq(common, chess, game, analyse, db)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, semver)
  )

  lazy val security = project("security", Seq(common, hub, db, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, maxmind, hasher)
  )

  lazy val shutup = project("shutup", Seq(common, db, hub, game, relation)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val challenge = project("challenge", Seq(common, db, hub, setup, game)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val study = project("study", Seq(
    common, db, hub, socket, game, round, importer, notifyModule, relation, evalCache)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val studySearch = project("studySearch", Seq(common, hub, study, search)).settings(
    libraryDependencies ++= provided(
      play.api,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val learn = project("learn", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val evalCache = project("evalCache", Seq(common, db, user, security, socket, tree)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val practice = project("practice", Seq(common, db, memo, user, study)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver, configs)
  )

  lazy val playban = project("playban", Seq(common, db, game)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val push = project("push", Seq(common, db, user, game, challenge, message)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val slack = project("slack", Seq(common, hub, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val plan = project("plan", Seq(common, user, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val relation = project("relation", Seq(common, db, memo, hub, user, game, pref)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val pref = project("pref", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val message = project("message", Seq(common, db, user, hub, relation, security, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val forum = project("forum", Seq(common, db, user, security, hub, mod, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val forumSearch = project("forumSearch", Seq(common, hub, forum, search)).settings(
    libraryDependencies ++= provided(
      play.api,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val team = project("team", Seq(common, memo, db, user, forum, security, hub, notifyModule)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val teamSearch = project("teamSearch", Seq(common, hub, team, search)).settings(
    libraryDependencies ++= provided(
      play.api,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val i18n = project("i18n", Seq(common, db, user, hub)).settings(
    sourceGenerators in Compile += Def.task {
      MessageCompiler(
        (baseDirectory in Compile).value / "messages",
        (sourceManaged in Compile).value / "messages"
      )
    }.taskValue,
    libraryDependencies ++= provided(play.api, reactivemongo.driver, jgit)
  )

  lazy val bookmark = project("bookmark", Seq(common, memo, db, hub, user, game)).settings(
    libraryDependencies ++= provided(
      play.api, play.test, reactivemongo.driver)
  )

  lazy val report = project("report", Seq(common, db, user)).settings(
    libraryDependencies ++= provided(
      play.api, reactivemongo.driver)
  )

  lazy val explorer = project("explorer", Seq(common, db, game)).settings(
    libraryDependencies ++= provided(
      play.api,
      reactivemongo.driver, reactivemongo.iteratees)
  )

  lazy val notifyModule = project("notify", Seq(common, db, game, user, hub, relation)).settings(
    libraryDependencies ++= provided(play.api, reactivemongo.driver)
  )

  lazy val site = project("site", Seq(common, socket)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val tree = project("tree", Seq(chess)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val socket = project("socket", Seq(common, hub, memo, tree)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val hub = project("hub", Seq(common, chess)).settings(
    libraryDependencies ++= provided(play.api)
  )

  lazy val chess = project("chess")
}
