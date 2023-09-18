package lila.relay

import akka.actor.*
import com.softwaremill.macwire.*
import com.softwaremill.tagging.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*

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
    irc: lila.irc.IrcApi,
    baseUrl: BaseUrl
)(using
    ec: Executor,
    system: ActorSystem,
    scheduler: Scheduler,
    materializer: akka.stream.Materializer
):

  lazy val roundForm = wire[RelayRoundForm]

  lazy val tourForm = wire[RelayTourForm]

  private val colls = wire[RelayColls]

  private lazy val roundRepo = RelayRoundRepo(colls.round)

  private lazy val tourRepo = RelayTourRepo(colls.tour)

  private lazy val leaderboard = wire[RelayLeaderboardApi]

  lazy val jsonView = wire[JsonView]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val pager = wire[RelayPager]

  lazy val push = wire[RelayPush]

  lazy val markup = wire[RelayMarkup]

  lazy val pgnStream = wire[RelayPgnStream]

  private lazy val sync = wire[RelaySync]

  private lazy val formatApi = wire[RelayFormatApi]

  private lazy val delay = wire[RelayDelay]

  // start the sync scheduler
  wire[RelayFetch]

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute): () =>
    api.autoStart >> api.autoFinishNotSyncing

  lila.common.Bus.subscribeFuns(
    "study" -> { case lila.hub.actorApi.study.RemoveStudy(studyId) =>
      api.onStudyRemove(studyId)
    },
    "relayToggle" -> { case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi.isContributor(id, who.u) foreach {
        _ so api.requestPlay(id into RelayRoundId, v)
      }
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
  val round = mainDb(CollName("relay"))
  val tour  = mainDb(CollName("relay_tour"))
  val delay = yoloDb(CollName("relay_delay"))
