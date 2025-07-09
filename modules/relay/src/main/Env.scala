package lila.relay

import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.paginator.Paginator

import scala.util.matching.Regex

import lila.core.config.*

import lila.common.Bus
import lila.memo.SettingStore
import lila.memo.SettingStore.Formable.given
import lila.relay.RelayTour.WithLastRound
import lila.core.id.RelayRoundId

@Module
final class Env(
    config: play.api.Configuration,
    ws: StandaloneWSClient,
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    studyApi: lila.study.StudyApi,
    studyRepo: lila.study.StudyRepo,
    chapterPreview: lila.study.ChapterPreviewApi,
    chapterRepo: lila.study.ChapterRepo,
    studyPgnDump: lila.study.PgnDump,
    gameRepo: lila.game.GameRepo,
    pgnDump: lila.game.PgnDump,
    gameProxy: lila.core.game.GameProxy,
    guessPlayer: lila.core.fide.GuessPlayer,
    getPlayer: lila.core.fide.GetPlayer,
    getPlayerFollowers: lila.core.fide.GetPlayerFollowers,
    cacheApi: lila.memo.CacheApi,
    settingStore: SettingStore.Builder,
    irc: lila.core.irc.IrcApi,
    baseUrl: BaseUrl,
    notifyApi: lila.core.notify.NotifyApi,
    picfitApi: lila.memo.PicfitApi,
    picfitUrl: lila.memo.PicfitUrl,
    lightUserSync: lila.core.LightUser.GetterSync,
    langList: lila.core.i18n.LangList,
    baker: lila.core.security.LilaCookie
)(using Executor, akka.stream.Materializer, play.api.Mode)(using scheduler: Scheduler):

  lazy val roundForm = wire[RelayRoundForm]
  lazy val groupForm = wire[RelayGroupForm]
  lazy val tourForm  = wire[RelayTourForm]

  private val colls = wire[RelayColls]

  private lazy val roundRepo = RelayRoundRepo(colls.round)

  private lazy val tourRepo = RelayTourRepo(colls.tour)

  private lazy val groupRepo = RelayGroupRepo(colls.group)

  private lazy val groupCrowd = wire[RelayGroupCrowdSumCache]

  private lazy val playerEnrich = wire[RelayPlayerEnrich]

  private lazy val notifyAdmin = wire[RelayNotifierAdmin]

  private lazy val notifier = wire[RelayNotifier]

  private lazy val studyPropagation = wire[RelayStudyPropagation]

  private lazy val tagManualOverride = wire[RelayTagManualOverride]

  lazy val jsonView = wire[JsonView]

  lazy val listing = wire[RelayListing]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val tourStream: RelayTourStream = wire[RelayTourStream]

  lazy val pager = wire[RelayPager]

  lazy val calendar = wire[RelayCalendar]

  lazy val push = wire[RelayPush]

  lazy val markup = wire[RelayMarkup]

  lazy val pgnStream = wire[RelayPgnStream]

  lazy val teamTable = wire[RelayTeamTable]

  lazy val playerTour = wire[RelayPlayerTour]

  lazy val playerApi = wire[RelayPlayerApi]

  lazy val videoEmbed = wire[lila.relay.RelayVideoEmbedStore]

  def top(page: Int): Fu[(List[RelayCard], Paginator[WithLastRound])] =
    (page == 1).so(listing.active).zip(pager.inactive(page))

  private lazy val sync = wire[RelaySync]

  private lazy val proxy                 = wire[RelayProxy]
  private def selectProxy: ProxySelector = proxy.select

  private lazy val httpClient = wire[HttpClient]

  private lazy val formatApi = wire[RelayFormatApi]

  private lazy val delay = wire[RelayDelay]

  // eager init to start the scheduler
  private val stats = wire[RelayStatsApi]
  export stats.getJson as statsJson

  import SettingStore.CredentialsOption.given
  val proxyCredentials = settingStore[Option[Credentials]](
    "relayProxyCredentials",
    default = none,
    text =
      "Broadcast: proxy credentials to fetch from external sources. Leave empty to use no auth (?!). Format: username:password".some
  ).taggedWith[ProxyCredentials]

  import SettingStore.HostPortOption.given
  val proxyHostPort = settingStore[Option[HostPort]](
    "relayProxyHostPort",
    default = none,
    text =
      "Broadcast: proxy host and port to fetch from external sources. Leave empty to use no proxy. Format: host:port".some
  ).taggedWith[ProxyHostPort]

  import SettingStore.Regex.given
  val proxyDomainRegex = settingStore[Regex](
    "relayProxyDomainRegex",
    default = "-".r,
    text = "Broadcast: source domains that use a proxy, as a regex".some
  ).taggedWith[ProxyDomainRegex]

  private val relayFidePlayerApi = wire[RelayFidePlayerApi]

  import lila.common.config.given
  private val syncOnlyIds = config.getOptional[List[String]]("relay.syncOnlyIds").map(RelayTourId.from)

  // start the sync scheduler
  wire[RelayFetch]

  scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
    api.autoStart >> api.autoFinishNotSyncing(syncOnlyIds)

  Bus.sub[lila.core.study.RemoveStudy]: s =>
    api.onStudyRemove(s.studyId)

  Bus.sub[lila.study.RelayToggle]:
    case lila.study.RelayToggle(id, v, who) =>
      studyApi
        .isContributor(id, who.u)
        .foreach:
          _.so(api.requestPlay(id.into(RelayRoundId), v, s"manual toggle by ${who.u}"))

  Bus.sub[lila.study.Kick]:
    case lila.study.Kick(studyId, userId, who) =>
      roundRepo.tourIdByStudyId(studyId).flatMapz(api.kickBroadcast(userId, _, who))

  Bus.sub[lila.study.BecomeStudyAdmin]:
    case lila.study.BecomeStudyAdmin(studyId, me) =>
      api.becomeStudyAdmin(studyId, me)

  Bus.sub[lila.study.IsOfficialRelay]:
    case lila.study.IsOfficialRelay(studyId, promise) =>
      promise.completeWith(api.isOfficial(studyId.into(RelayRoundId)))

  Bus.sub[lila.study.StudyMembers.OnChange]: change =>
    studyPropagation.onStudyMembersChange(change.study)

  Bus.sub[lila.core.study.GetRelayCrowd]:
    case lila.core.study.GetRelayCrowd(studyId, promise) =>
      roundRepo.currentCrowd(studyId.into(RelayRoundId)).map(_.orZero).foreach(promise.success)

private final class RelayColls(mainDb: lila.db.Db, yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb):
  val round = mainDb(CollName("relay"))
  val tour  = mainDb(CollName("relay_tour"))
  val group = mainDb(CollName("relay_group"))
  val delay = yoloDb(CollName("relay_delay"))
  val stats = mainDb(CollName("relay_stats"))

private trait ProxyCredentials
private trait ProxyHostPort
private trait ProxyDomainRegex
