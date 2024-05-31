package lila.api

import akka.actor._
import com.softwaremill.macwire._
import play.api.libs.ws.WSClient
import play.api.{ Configuration, Mode }
import scala.concurrent.duration._

import lila.common.config._
import lila.user.User

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    securityEnv: lila.security.Env,
    teamSearchEnv: lila.teamSearch.Env,
    forumSearchEnv: lila.forumSearch.Env,
    teamEnv: lila.team.Env,
    puzzleEnv: lila.puzzle.Env,
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
    onlineApiUsers: lila.bot.OnlineApiUsers,
    challengeEnv: lila.challenge.Env,
    socketEnv: lila.socket.Env,
    msgEnv: lila.msg.Env,
    timelineEnv: lila.timeline.Env,
    cacheApi: lila.memo.CacheApi,
    ws: WSClient,
    val mode: Mode
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  val config = ApiConfig loadFrom appConfig
  import config.apiToken
  import net.baseUrl

  lazy val notationDump: NotationDump = wire[NotationDump]

  lazy val userApi = wire[UserApi]

  lazy val gameApi = wire[GameApi]

  lazy val realPlayers = wire[RealPlayerApi]

  lazy val gameApiV2 = wire[GameApiV2]

  lazy val userGameApi = wire[UserGameApi]

  lazy val roundApi = wire[RoundApi]

  lazy val lobbyApi = wire[LobbyApi]

  lazy val eventStream = wire[EventStream]

  lazy val referrerRedirect = wire[ReferrerRedirect]

  lazy val cli = wire[Cli]

  lazy val influxEvent = new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  // if (mode == Mode.Prod) system.scheduler.scheduleOnce(5 seconds)(influxEvent.start())

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size)
    socketEnv.remoteSocket.onlineUserIds.getAndUpdate(_ + User.lishogiId)
    userEnv.repo.setSeenAt(User.lishogiId)
  }
}
