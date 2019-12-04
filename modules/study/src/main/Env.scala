package lila.study

import akka.actor._
import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.WSClient
import scala.concurrent.duration._

import lila.common.config._
import lila.hub.{ Duct, DuctMap }
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User

final class Env(
    appConfig: Configuration,
    ws: WSClient,
    lightUserApi: lila.user.LightUserApi,
    gamePgnDump: lila.game.PgnDump,
    divider: lila.game.Divider,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    importer: lila.importer.Importer,
    explorerImporter: lila.explorer.ExplorerImporter,
    notifyApi: lila.notify.NotifyApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    timeline: lila.hub.actors.Timeline,
    fishnet: lila.hub.actors.Fishnet,
    chatApi: lila.chat.ChatApi,
    db: lila.db.Env,
    net: lila.common.config.NetConfig,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit system: akka.actor.ActorSystem) {

  def version(studyId: Study.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](studyId.value)(GetVersion)

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socket.isPresent(studyId, userId)

  private def scheduler = system.scheduler

  private val socket = wire[StudySocket]

  private lazy val studyRepo = new StudyRepo(db(CollName("study")))
  private lazy val chapterRepo = new ChapterRepo(db(CollName("study_chapter")))

  lazy val jsonView = wire[JsonView]

  private lazy val pgnFetch = wire[PgnFetch]

  private lazy val chapterMaker = wire[ChapterMaker]

  private lazy val explorerGame = wire[ExplorerGame]

  private lazy val studyMaker = wire[StudyMaker]

  private lazy val studyInvite = wire[StudyInvite]

  private lazy val sequencers = new DuctMap(
    mkDuct = _ => Duct.extra.lazyPromise(5.seconds.some),
    accessTimeout = 10 minutes
  )

  private lazy val sequencer = wire[StudySequencer]

  private lazy val serverEvalRequester = wire[ServerEval.Requester]

  lazy val serverEvalMerger = wire[ServerEval.Merger]

  lazy val api: StudyApi = wire[StudyApi]

  lazy val pager = wire[StudyPager]

  private def runCommand = db.runCommand

  lazy val multiBoard = wire[StudyMultiBoard]

  lazy val pgnDump = wire[PgnDump]

  lazy val lightStudyCache: LightStudyCache = asyncCache.multi(
    name = "study.lightStudyCache",
    f = studyRepo.lightById,
    expireAfter = _.ExpireAfterWrite(20 minutes)
  )

  def cli = new lila.common.Cli {
    def process = {
      case "study" :: "rank" :: "reset" :: Nil => api.resetAllRanks.map { count => s"$count done" }
    }
  }

  lila.common.Bus.subscribeFun("gdprErase", "studyAnalysisProgress") {
    case lila.user.User.GDPRErase(user) => api erase user
    case lila.analyse.actorApi.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)
  }
}
