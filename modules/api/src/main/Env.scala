package lila.api

import akka.actor._
import com.softwaremill.macwire._
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Mode }
import scala.concurrent.duration._

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    securityEnv: lila.security.Env,
    teamSearchEnv: lila.teamSearch.Env,
    forumSearchEnv: lila.forumSearch.Env,
    teamEnv: lila.team.Env,
    puzzleEnv: lila.puzzle.Env,
    explorerEnv: lila.explorer.Env,
    fishnetEnv: lila.fishnet.Env,
    studyEnv: lila.study.Env,
    studySearchEnv: lila.studySearch.Env,
    coachEnv: lila.coach.Env,
    evalCacheEnv: lila.evalCache.Env,
    planEnv: lila.plan.Env,
    gameEnv: lila.game.Env,
    roundEnv: lila.round.Env,
    bookmarkApi: lila.bookmark.BookmarkApi,
    prefApi: lila.pref.PrefApi,
    playBanApi: lila.playban.PlaybanApi,
    userEnv: lila.user.Env,
    streamerEnv: lila.streamer.Env,
    relationEnv: lila.relation.Env,
    analyseEnv: lila.analyse.Env,
    lobbyEnv: lila.lobby.Env,
    simulEnv: lila.simul.Env,
    tourEnv: lila.tournament.Env,
    swissEnv: lila.swiss.Env,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    challengeEnv: lila.challenge.Env,
    msgEnv: lila.msg.Env,
    cacheApi: lila.memo.CacheApi,
    ws: WSClient,
    val mode: Mode
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  val config = ApiConfig loadFrom appConfig
  import config.apiToken

  lazy val pgnDump: PgnDump = wire[PgnDump]

  lazy val userApi = wire[UserApi]

  lazy val gameApi = wire[GameApi]

  lazy val realPlayers = wire[RealPlayerApi]

  lazy val gameApiV2 = wire[GameApiV2]

  lazy val userGameApi = wire[UserGameApi]

  lazy val roundApi = wire[RoundApi]

  lazy val lobbyApi = wire[LobbyApi]

  lazy val eventStream = wire[EventStream]

  lazy val cli = wire[Cli]

  lazy val influxEvent = new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  if (mode == Mode.Prod && false) system.scheduler.scheduleOnce(5 seconds)(influxEvent.start()) // yep...

  system.scheduler.scheduleWithFixedDelay(20 seconds, 10 seconds) { () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size)
  }
}
