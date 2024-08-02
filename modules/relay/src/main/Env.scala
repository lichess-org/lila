package lila.relay

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.libs.ws.StandaloneWSClient
import scalalib.paginator.Paginator

import scala.util.matching.Regex

import lila.core.config.*
import lila.memo.SettingStore
import lila.memo.SettingStore.Formable.given
import lila.relay.RelayTour.{ ActiveWithSomeRounds, WithLastRound }

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
    federations: lila.core.fide.Federation.FedsOf,
    guessPlayer: lila.core.fide.GuessPlayer,
    cacheApi: lila.memo.CacheApi,
    settingStore: SettingStore.Builder,
    irc: lila.core.irc.IrcApi,
    baseUrl: BaseUrl,
    notifyApi: lila.core.notify.NotifyApi,
    picfitApi: lila.memo.PicfitApi,
    picfitUrl: lila.memo.PicfitUrl,
    langList: lila.core.i18n.LangList
)(using Executor, ActorSystem, akka.stream.Materializer, play.api.Mode, lila.core.i18n.Translator)(using
    scheduler: Scheduler
):

  lazy val roundForm = wire[RelayRoundForm]

  lazy val tourForm = wire[RelayTourForm]

  private val colls = wire[RelayColls]

  private lazy val roundRepo = RelayRoundRepo(colls.round)

  private lazy val tourRepo = RelayTourRepo(colls.tour)

  private lazy val groupRepo = RelayGroupRepo(colls.group)

  lazy val leaderboard = wire[RelayLeaderboardApi]

  private lazy val playersApi = wire[RelayPlayersApi]

  private lazy val notifyMissingFideIds = wire[RelayNotifyMissingFideIds]

  private lazy val notifyOrphanBoard = wire[RelayNotifyOrphanBoard]

  private lazy val notifier = wire[RelayNotifier]

  lazy val jsonView = wire[JsonView]

  lazy val listing: RelayListing = wire[RelayListing]

  lazy val stats = wire[RelayStatsApi]

  private lazy val playersUpdate = wire[RelayPlayersUpdate]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val tourStream: RelayTourStream = wire[RelayTourStream]

  lazy val pager = wire[RelayPager]

  lazy val push = wire[RelayPush]

  lazy val markup = wire[RelayMarkup]

  lazy val pgnStream = wire[RelayPgnStream]

  lazy val teamTable = wire[RelayTeamTable]

  lazy val playerTour = wire[RelayPlayerTour]

  def top(page: Int): Fu[(List[ActiveWithSomeRounds], List[WithLastRound], Paginator[WithLastRound])] = for
    active   <- (page == 1).so(listing.active.get({}))
    upcoming <- (page == 1).so(listing.upcoming.get({}))
    past     <- pager.inactive(page)
  yield (active, upcoming, past)

  private lazy val sync = wire[RelaySync]

  private lazy val formatApi = wire[RelayFormatApi]

  private lazy val delay = wire[RelayDelay]

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

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    api.autoStart >> api.autoFinishNotSyncing

  lila.common.Bus.subscribeFuns(
    "study" -> { case lila.core.study.RemoveStudy(studyId) =>
      api.onStudyRemove(studyId)
    },
    "relayToggle" -> { case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi
        .isContributor(id, who.u)
        .foreach:
          _.so(api.requestPlay(id.into(RelayRoundId), v))
    },
    "kickStudy" -> { case lila.study.actorApi.Kick(studyId, userId, who) =>
      roundRepo.tourIdByStudyId(studyId).flatMapz(api.kickBroadcast(userId, _, who))
    },
    "adminStudy" -> { case lila.study.actorApi.BecomeStudyAdmin(studyId, me) =>
      api.becomeStudyAdmin(studyId, me)
    },
    "isOfficialRelay" -> { case lila.study.actorApi.IsOfficialRelay(studyId, promise) =>
      promise.completeWith(api.isOfficial(studyId.into(RelayRoundId)))
    }
  )

private class RelayColls(mainDb: lila.db.Db, yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb):
  val round = mainDb(CollName("relay"))
  val tour  = mainDb(CollName("relay_tour"))
  val group = mainDb(CollName("relay_group"))
  val delay = yoloDb(CollName("relay_delay"))
  val stats = mainDb(CollName("relay_stats"))

private trait ProxyCredentials
private trait ProxyHostPort
private trait ProxyDomainRegex
