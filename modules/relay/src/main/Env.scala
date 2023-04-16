package lila.relay

import akka.actor.*
import com.softwaremill.macwire.*
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*

@Module
@annotation.nowarn("msg=unused")
final class Env(
    ws: StandaloneWSClient,
    db: lila.db.Db,
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

  private lazy val roundRepo = new RelayRoundRepo(db(CollName("relay")))

  private lazy val tourRepo = new RelayTourRepo(db(CollName("relay_tour")))

  private lazy val leaderboard = wire[RelayLeaderboardApi]

  lazy val jsonView = wire[JsonView]

  lazy val api: RelayApi = wire[RelayApi]

  lazy val pager = wire[RelayPager]

  lazy val push = wire[RelayPush]

  lazy val markup = wire[RelayMarkup]

  lazy val pgnStream = wire[RelayPgnStream]

  private lazy val sync = wire[RelaySync]

  private lazy val formatApi = wire[RelayFormatApi]

  // start the sync scheduler
  wire[RelayFetch]

  system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
    api.autoStart >> api.autoFinishNotSyncing
    ()
  }

  lila.common.Bus.subscribeFuns(
    "study" -> { case lila.hub.actorApi.study.RemoveStudy(studyId, _) =>
      api.onStudyRemove(studyId).unit
    },
    "relayToggle" -> { case lila.study.actorApi.RelayToggle(id, v, who) =>
      studyApi.isContributor(id, who.u) foreach {
        _ ?? api.requestPlay(id into RelayRoundId, v)
      }
    },
    "isOfficialRelay" -> { case lila.study.actorApi.IsOfficialRelay(studyId, promise) =>
      promise completeWith api.isOfficial(studyId)
    }
  )
