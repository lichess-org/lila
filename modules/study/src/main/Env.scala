package lila.study

import com.softwaremill.macwire._
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config._
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    lightUserApi: lila.user.LightUserApi,
    gamePgnDump: lila.game.PgnDump,
    divider: lila.game.Divider,
    gameRepo: lila.game.GameRepo,
    userRepo: lila.user.UserRepo,
    explorerImporter: lila.explorer.ExplorerImporter,
    notifyApi: lila.notify.NotifyApi,
    prefApi: lila.pref.PrefApi,
    relationApi: lila.relation.RelationApi,
    remoteSocketApi: lila.socket.RemoteSocket,
    timeline: lila.hub.actors.Timeline,
    fishnet: lila.hub.actors.Fishnet,
    chatApi: lila.chat.ChatApi,
    db: lila.db.Db,
    net: lila.common.config.NetConfig,
    cacheApi: lila.memo.CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
) {

  def version(studyId: Study.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](studyId.value)(GetVersion)

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socket.isPresent(studyId, userId)

  private def scheduler = system.scheduler

  private val socket = wire[StudySocket]

  lazy val studyRepo             = new StudyRepo(db(CollName("study")))
  lazy val chapterRepo           = new ChapterRepo(db(CollName("study_chapter")))
  private lazy val topicRepo     = new StudyTopicRepo(db(CollName("study_topic")))
  private lazy val userTopicRepo = new StudyUserTopicRepo(db(CollName("study_user_topic")))

  lazy val jsonView = wire[JsonView]

  private lazy val pgnFetch = wire[PgnFetch]

  private lazy val chapterMaker = wire[ChapterMaker]

  private lazy val explorerGame = wire[ExplorerGame]

  private lazy val studyMaker = wire[StudyMaker]

  private lazy val studyInvite = wire[StudyInvite]

  private lazy val serverEvalRequester = wire[ServerEval.Requester]

  private lazy val sequencer = wire[StudySequencer]

  lazy val serverEvalMerger = wire[ServerEval.Merger]

  lazy val topicApi = wire[StudyTopicApi]

  lazy val api: StudyApi = wire[StudyApi]

  lazy val pager = wire[StudyPager]

  private def runCommand = db.runCommand

  lazy val multiBoard = wire[StudyMultiBoard]

  lazy val pgnDump = wire[PgnDump]

  lazy val gifExport = new GifExport(ws, appConfig.get[String]("game.gifUrl"))

  def cli =
    new lila.common.Cli {
      def process = {
        case "study" :: "rank" :: "reset" :: Nil =>
          api.resetAllRanks.map { count =>
            s"$count done"
          }
      }
    }

  lila.common.Bus.subscribeFun("gdprErase", "studyAnalysisProgress") {
    case lila.user.User.GDPRErase(user) => api erase user
    case lila.analyse.actorApi.StudyAnalysisProgress(analysis, complete) =>
      serverEvalMerger(analysis, complete)
  }
}
