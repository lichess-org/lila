package lila.api

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.{ Configuration, Mode }

import lila.chat.{ GetLinkCheck, IsChatFresh }
import lila.common.Bus
import lila.core.misc.lpv.*

@Module
final class Env(
    appConfig: Configuration,
    net: lila.core.config.NetConfig,
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
    fideEnv: lila.fide.Env,
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
    shutupEnv: lila.shutup.Env,
    titleEnv: lila.title.Env,
    modLogApi: lila.mod.ModlogApi,
    activityWriteApi: lila.activity.ActivityWriteApi,
    ublogApi: lila.ublog.UblogApi,
    picfitUrl: lila.memo.PicfitUrl,
    cmsApi: lila.cms.CmsApi,
    cacheApi: lila.memo.CacheApi,
    webConfig: lila.web.WebConfig,
    realPlayerApi: lila.web.RealPlayerApi,
    bookmarkExists: lila.core.bookmark.BookmarkExists,
    manifest: lila.web.AssetManifest
)(using val mode: Mode, scheduler: Scheduler)(using
    Executor,
    ActorSystem,
    akka.stream.Materializer,
    lila.core.i18n.Translator
):

  export net.{ baseUrl, domain }

  lazy val pgnDump: PgnDump = wire[PgnDump]

  lazy val textLpvExpand = wire[TextLpvExpand]

  lazy val userApi = wire[UserApi]

  export webConfig.apiToken
  lazy val gameApi = wire[GameApi]

  lazy val gameApiV2 = wire[GameApiV2]

  lazy val roundApi = wire[RoundApi]

  lazy val lobbyApi = wire[LobbyApi]

  lazy val eventStream = wire[EventStream]

  lazy val personalDataExport = wire[PersonalDataExport]

  lazy val accountClosure = wire[AccountClosure]

  lazy val cli = wire[Cli]

  private lazy val linkCheck = wire[LinkCheck]
  lazy val chatFreshness     = wire[ChatFreshness]

  Bus.subscribeFuns(
    "chatLinkCheck" -> { case GetLinkCheck(line, source, promise) =>
      promise.completeWith(linkCheck(line, source))
    },
    "chatFreshness" -> { case IsChatFresh(source, promise) =>
      promise.completeWith(chatFreshness.of(source))
    },
    "lpv" -> {
      case AllPgnsFromText(text, p)       => p.completeWith(textLpvExpand.allPgnsFromText(text))
      case LpvLinkRenderFromText(text, p) => p.completeWith(textLpvExpand.linkRenderFromText(text))
    }
  )

  lila.i18n.Registry.asyncLoadLanguages()

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size())
    lila.mon.jvm.threads()
    // ensure the Lichess user is online
    socketEnv.remoteSocket.onlineUserIds.getAndUpdate(_ + UserId.lichess)
    userEnv.repo.setSeenAt(UserId.lichess)
