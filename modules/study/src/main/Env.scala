package lila.study

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.common.config.*
import lila.socket.{ GetVersion, SocketVersion }
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
    analyser: lila.analyse.Analyser,
    annotator: lila.analyse.Annotator,
    mongo: lila.db.Env,
    net: lila.common.config.NetConfig,
    cacheApi: lila.memo.CacheApi
)(using
    ec: scala.concurrent.ExecutionContext,
    scheduler: akka.actor.Scheduler,
    mat: akka.stream.Materializer,
    mode: play.api.Mode
):

  private lazy val studyDb = mongo.asyncDb("study", appConfig.get[String]("study.mongodb.uri"))

  def version(studyId: StudyId): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](studyId into RoomId)(GetVersion.apply)

  def isConnected(studyId: StudyId, userId: UserId): Fu[Boolean] =
    socket.isPresent(studyId, userId)

  private val socket: StudySocket = wire[StudySocket]

  lazy val studyRepo             = new StudyRepo(studyDb(CollName("study")))
  lazy val chapterRepo           = new ChapterRepo(studyDb(CollName("study_chapter_flat")))
  private lazy val topicRepo     = new StudyTopicRepo(studyDb(CollName("study_topic")))
  private lazy val userTopicRepo = new StudyUserTopicRepo(studyDb(CollName("study_user_topic")))

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

  lazy val multiBoard = wire[StudyMultiBoard]

  lazy val pgnDump = wire[PgnDump]

  lazy val gifExport = new GifExport(ws, appConfig.get[String]("game.gifUrl"))

  def cli =
    new lila.common.Cli:
      def process = { case "study" :: "rank" :: "reset" :: Nil =>
        api.resetAllRanks.map { count =>
          s"$count done"
        }
      }

  lila.common.Bus.subscribeFun("studyAnalysisProgress") {
    case lila.analyse.actorApi.StudyAnalysisProgress(analysis, complete) =>
      serverEvalMerger(analysis, complete).unit
  }
