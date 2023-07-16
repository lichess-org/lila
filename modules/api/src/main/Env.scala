package lila.api

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient
import play.api.{ Configuration, Mode }

import lila.chat.{ GetLinkCheck, IsChatFresh }
import lila.common.Bus
import lila.common.config.*
import lila.hub.actorApi.Announce
import lila.hub.actorApi.lpv.*
import lila.user.User

@Module
final class Env(
    appConfig: Configuration,
    net: NetConfig,
    securityEnv: lila.security.Env,
    mailerEnv: lila.mailer.Env,
    teamSearchEnv: lila.teamSearch.Env,
    forumSearchEnv: lila.forumSearch.Env,
    forumEnv: lila.forum.Env,
    teamEnv: lila.team.Env,
    puzzleEnv: lila.puzzle.Env,
    fishnetEnv: lila.fishnet.Env,
    studyEnv: lila.study.Env,
    studySearchEnv: lila.studySearch.Env,
    coachEnv: lila.coach.Env,
    evalCacheEnv: lila.evalCache.Env,
    planEnv: lila.plan.Env,
    gameEnv: lila.game.Env,
    chatEnv: lila.chat.Env,
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
    socketEnv: lila.socket.Env,
    msgEnv: lila.msg.Env,
    videoEnv: lila.video.Env,
    pushEnv: lila.push.Env,
    reportEnv: lila.report.Env,
    modEnv: lila.mod.Env,
    notifyEnv: lila.notify.Env,
    appealApi: lila.appeal.AppealApi,
    activityWriteApi: lila.activity.ActivityWriteApi,
    ublogApi: lila.ublog.UblogApi,
    picfitUrl: lila.memo.PicfitUrl,
    cacheApi: lila.memo.CacheApi,
    ws: StandaloneWSClient,
    val mode: Mode
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    materializer: akka.stream.Materializer
):

  val config = ApiConfig loadFrom appConfig
  export config.{ apiToken, pagerDuty as pagerDutyConfig }
  export net.{ baseUrl, domain }

  lazy val pgnDump: PgnDump = wire[PgnDump]

  lazy val textLpvExpand = wire[TextLpvExpand]

  lazy val userApi = wire[UserApi]

  lazy val gameApi = wire[GameApi]

  lazy val realPlayers = wire[RealPlayerApi]

  lazy val gameApiV2 = wire[GameApiV2]

  lazy val userGameApi = wire[UserGameApi]

  lazy val roundApi = wire[RoundApi]

  lazy val lobbyApi = wire[LobbyApi]

  lazy val eventStream = wire[EventStream]

  lazy val personalDataExport = wire[PersonalDataExport]

  lazy val referrerRedirect = wire[ReferrerRedirect]

  lazy val accountClosure = wire[AccountClosure]

  lazy val forumAccess = wire[ForumAccess]

  lazy val cli = wire[Cli]

  private lazy val influxEvent = new InfluxEvent(
    ws = ws,
    endpoint = config.influxEventEndpoint,
    env = config.influxEventEnv
  )
  if mode == Mode.Prod then scheduler.scheduleOnce(5 seconds)(influxEvent.start())

  private lazy val linkCheck = wire[LinkCheck]
  lazy val chatFreshness     = wire[ChatFreshness]

  private lazy val pagerDuty = wire[PagerDuty]

  Bus.subscribeFuns(
    "chatLinkCheck" -> { case GetLinkCheck(line, source, promise) =>
      promise completeWith linkCheck(line, source)
    },
    "chatFreshness" -> { case IsChatFresh(source, promise) =>
      promise completeWith chatFreshness.of(source)
    },
    "announce" -> {
      case Announce(msg, date, _) if msg contains "will restart" => pagerDuty.lilaRestart(date)
    },
    "lpv" -> {
      case AllPgnsFromText(text, p)       => p completeWith textLpvExpand.allPgnsFromText(text)
      case LpvLinkRenderFromText(text, p) => p completeWith textLpvExpand.linkRenderFromText(text)
    }
  )

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size())
    lila.mon.jvm.threads()
    // ensure the Lichess user is online
    socketEnv.remoteSocket.onlineUserIds.getAndUpdate(_ + User.lichessId)
    userEnv.repo.setSeenAt(User.lichessId)
