package lila.api

import akka.actor._
import com.softwaremill.macwire._
import play.api.libs.ws.WSClient
import play.api.{ Mode, Configuration }

import lila.common.config._

@Module
final class Env(
    appConfig: Configuration,
    netConfig: NetConfig,
    securityEnv: lila.security.Env,
    i18nEnv: lila.i18n.Env,
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
    settingStore: lila.memo.SettingStore.Builder,
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
    setupEnv: lila.setup.Env,
    simulEnv: lila.simul.Env,
    tourEnv: lila.tournament.Env,
    onlineBots: lila.bot.OnlineBots,
    pools: List[lila.pool.PoolConfig],
    challengeEnv: lila.challenge.Env,
    ws: WSClient,
    val mode: Mode
)(implicit system: ActorSystem) {

  val config = appConfig.get[ApiConfig]("")(ApiConfig.loader)

  lazy val pgnDump: PgnDump = wire[PgnDump]

  lazy val userApi = wire[UserApi]

  lazy val gameApi = wire[GameApi]

  lazy val gameApiV2 = wire[GameApiV2]

  lazy val userGameApi = wire[UserGameApi]

  lazy val roundApi = wire[RoundApi]

  lazy val lobbyApi = wire[LobbyApi]

  lazy val eventStream = wire[EventStream]

  lazy val cli = wire[Cli]

  KamonPusher.start(system)

  if (config.influxEventEnv != "dev") system.actorOf(Props(new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )), name = "influx-event")

  system.registerOnTermination {
    lila.common.Bus.publish(lila.hub.actorApi.Shutdown, "shutdown")
  }
}
