package lila.api

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.Mode

import lila.chat.{ GetLinkCheck, IsChatFresh }
import lila.common.Bus
import lila.core.misc.lpv.Lpv

@Module
final class Env(
    net: lila.core.config.NetConfig,
    securityEnv: lila.security.Env,
    mailerEnv: lila.mailer.Env,
    forumEnv: lila.forum.Env,
    teamEnv: lila.team.Env,
    puzzleEnv: lila.puzzle.Env,
    studyEnv: lila.study.Env,
    gameSearch: lila.gameSearch.GameSearchApi,
    coachEnv: lila.coach.Env,
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
    relayEnv: lila.relay.Env,
    analyseEnv: lila.analyse.Env,
    lobbyEnv: lila.lobby.Env,
    simulEnv: lila.simul.Env,
    tourEnv: lila.tournament.Env,
    swissEnv: lila.swiss.Env,
    onlineApiUsers: lila.bot.OnlineApiUsers,
    challengeEnv: lila.challenge.Env,
    socketEnv: lila.socket.Env,
    msgEnv: lila.msg.Env,
    pushEnv: lila.push.Env,
    reportEnv: lila.report.Env,
    modEnv: lila.mod.Env,
    appealApi: lila.appeal.AppealApi,
    shutupEnv: lila.shutup.Env,
    titleEnv: lila.title.Env,
    fideEnv: lila.fide.Env,
    modLogApi: lila.mod.ModlogApi,
    activityWriteApi: lila.activity.ActivityWriteApi,
    ublogApi: lila.ublog.UblogApi,
    picfitUrl: lila.memo.PicfitUrl,
    cacheApi: lila.memo.CacheApi,
    webConfig: lila.web.WebConfig,
    manifest: lila.web.AssetManifest,
    tokenApi: lila.oauth.AccessTokenApi,
    tv: lila.tv.Tv,
    activityRead: lila.activity.ActivityReadApi,
    activityJson: lila.activity.JsonView,
    webMobile: lila.web.Mobile
)(using scheduler: Scheduler)(using
    Mode,
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

  lazy val accountTermination = wire[AccountTermination]

  lazy val anySearch = wire[AnySearch]

  lazy val modTimeline = wire[ModTimelineApi]

  lazy val cli = wire[Cli]

  lazy val mobile = wire[MobileApi]

  private lazy val linkCheck = wire[LinkCheck]
  lazy val chatFreshness = wire[ChatFreshness]

  Bus.sub[GetLinkCheck]:
    case GetLinkCheck(line, source, promise) =>
      promise.completeWith(linkCheck(line, source))
  Bus.sub[IsChatFresh]:
    case IsChatFresh(source, promise) =>
      promise.completeWith(chatFreshness.of(source))
  Bus.sub[Lpv]:
    case Lpv.AllPgnsFromText(text, max, p) => p.completeWith(textLpvExpand.allPgnsFromText(text, max))
    case Lpv.LinkRenderFromText(text, p) => p.completeWith(textLpvExpand.linkRenderFromText(text))
  Bus.sub[lila.core.security.GarbageCollect]: gc =>
    accountTermination.garbageCollect(gc.userId)
  Bus.sub[lila.core.playban.RageSitClose]: close =>
    accountTermination.lichessDisable(close.userId)

  lila.i18n.Registry.asyncLoadLanguages()

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
    lila.mon.bus.classifiers.update(lila.common.Bus.size())
    lila.mon.jvm.threads()
    // ensure the Lichess user is online
    socketEnv.remoteSocket.onlineUserIds.getAndUpdate(_ + UserId.lichess)
    userEnv.repo.setSeenAt(UserId.lichess)
