package lila.relay

import akka.actor.*
import scala.util.matching.Regex
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.memo.SettingStore
import lila.memo.SettingStore.Formable.given

@Module
final class Env(
    ws: StandaloneWSClient,
    db: lila.db.Db,
    yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb,
    studyApi: lila.study.StudyApi,
    multiboard: lila.study.StudyMultiBoard,
    studyRepo: lila.study.StudyRepo,
    chapterRepo: lila.study.ChapterRepo,
    studyPgnDump: lila.study.PgnDump,
    gameRepo: lila.game.GameRepo,
    pgnDump: lila.game.PgnDump,
    gameProxy: lila.round.GameProxyRepo,
    cacheApi: lila.memo.CacheApi,
    settingStore: SettingStore.Builder,
    irc: lila.irc.IrcApi,
    baseUrl: BaseUrl,
    notifyApi: lila.notify.NotifyApi,
    picfitApi: lila.memo.PicfitApi,
    picfitUrl: lila.memo.PicfitUrl
)(using Executor, ActorSystem, akka.stream.Materializer, play.api.Mode)(using scheduler: Scheduler):

  lazy val roundForm = wire[RelayRoundForm]

  lazy val tourForm = wire[RelayTourForm]

  private val colls = wire[RelayColls]

  private lazy val roundRepo = RelayRoundRepo(colls.round)

  private lazy val tourRepo = RelayTourRepo(colls.tour)

  private lazy val groupRepo = RelayGroupRepo(colls.group)

  lazy val leaderboard = wire[RelayLeaderboardApi]

  private lazy val notifier = wire[RelayNotifier]

  lazy val jsonView = wire[JsonView]

  lazy val listing: RelayListing = wire[RelayListing]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val tourStream: RelayTourStream = wire[RelayTourStream]

  lazy val pager = wire[RelayPager]

  lazy val push = wire[RelayPush]

  lazy val markup = wire[RelayMarkup]

  lazy val pgnStream = wire[RelayPgnStream]

  lazy val teamTable = wire[RelayTeamTable]

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

  val fidePlayerApi            = wire[RelayFidePlayerApi]
  private val fidePlayerUpdate = wire[RelayFidePlayerUpdate]

  def cli = new lila.common.Cli:
    def process =
      case "relay" :: "fide" :: "player" :: "update" :: Nil =>
        fidePlayerUpdate()
        fuccess("Updating the database in the background.")

  // start the sync scheduler
  wire[RelayFetch]

  scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    api.autoStart >> api.autoFinishNotSyncing

  lila.common.Bus.subscribeFuns(
    "study" -> { case lila.hub.actorApi.study.RemoveStudy(studyId) =>
      api.onStudyRemove(studyId)
    },
    "relayToggle" -> { case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi
        .isContributor(id, who.u)
        .foreach:
          _ so api.requestPlay(id into RelayRoundId, v)
    },
    "kickStudy" -> { case lila.study.actorApi.Kick(studyId, userId, who) =>
      roundRepo.tourIdByStudyId(studyId).flatMapz(api.kickBroadcast(userId, _, who))
    },
    "adminStudy" -> { case lila.study.actorApi.BecomeStudyAdmin(studyId, me) =>
      api.becomeStudyAdmin(studyId, me)
    },
    "isOfficialRelay" -> { case lila.study.actorApi.IsOfficialRelay(studyId, promise) =>
      promise completeWith api.isOfficial(studyId)
    }
  )

private class RelayColls(mainDb: lila.db.Db, yoloDb: lila.db.AsyncDb @@ lila.db.YoloDb):
  val round      = mainDb(CollName("relay"))
  val tour       = mainDb(CollName("relay_tour"))
  val group      = mainDb(CollName("relay_group"))
  val delay      = yoloDb(CollName("relay_delay"))
  val fidePlayer = yoloDb(CollName("relay_fide_player"))

private trait ProxyCredentials
private trait ProxyHostPort
private trait ProxyDomainRegex
