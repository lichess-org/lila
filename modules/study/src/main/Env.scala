package lila.study

import com.softwaremill.macwire.*
import play.api.Configuration
import play.api.libs.ws.StandaloneWSClient

import lila.core.config.*
import lila.core.socket.{ GetVersion, SocketVersion }
import lila.core.user.FlairGet

@Module
final class Env(
    appConfig: Configuration,
    ws: StandaloneWSClient,
    lightUserApi: lila.core.user.LightUserApi,
    gamePgnDump: lila.core.game.PgnDump,
    divider: lila.core.game.Divider,
    gameRepo: lila.core.game.GameRepo,
    namer: lila.core.game.Namer,
    userApi: lila.core.user.UserApi,
    flairApi: lila.core.user.FlairApi,
    explorer: lila.core.game.Explorer,
    notifyApi: lila.core.notify.NotifyApi,
    federations: lila.core.fide.Federation.FedsOf,
    federationNames: lila.core.fide.Federation.NamesOf,
    prefApi: lila.core.pref.PrefApi,
    relationApi: lila.core.relation.RelationApi,
    socketKit: lila.core.socket.SocketKit,
    socketReq: lila.core.socket.SocketRequester,
    chatApi: lila.core.chat.ChatApi,
    analyser: lila.tree.Analyser,
    analysisJson: lila.tree.AnalysisJson,
    annotator: lila.tree.Annotator,
    mongo: lila.db.Env,
    net: lila.core.config.NetConfig,
    cacheApi: lila.memo.CacheApi
)(using
    FlairGet,
    Executor,
    Scheduler,
    akka.stream.Materializer,
    play.api.Mode,
    lila.core.i18n.Translator,
    lila.core.config.RateLimit
):

  private lazy val studyDb = mongo.asyncDb("study", appConfig.get[String]("study.mongodb.uri"))

  def version(studyId: StudyId): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](studyId.into(RoomId))(GetVersion.apply)

  def isConnected(studyId: StudyId, userId: UserId): Fu[Boolean] =
    socket.isPresent(studyId, userId)

  private lazy val socket: StudySocket = wire[StudySocket]

  val studyRepo             = StudyRepo(studyDb(CollName("study")))
  val chapterRepo           = ChapterRepo(studyDb(CollName("study_chapter_flat")))
  private val topicRepo     = StudyTopicRepo(studyDb(CollName("study_topic")))
  private val userTopicRepo = StudyUserTopicRepo(studyDb(CollName("study_user_topic")))

  lazy val jsonView = wire[JsonView]

  private lazy val pgnFetch = wire[PgnFetch]

  private lazy val chapterMaker = wire[ChapterMaker]

  private lazy val explorerGame = wire[ExplorerGameApi]

  private lazy val studyMaker = wire[StudyMaker]

  private lazy val studyInvite = wire[StudyInvite]

  private lazy val serverEvalRequester = wire[ServerEval.Requester]

  private lazy val sequencer = wire[StudySequencer]

  lazy val serverEvalMerger = wire[ServerEval.Merger]

  lazy val topicApi = wire[StudyTopicApi]

  lazy val api: StudyApi = wire[StudyApi]

  lazy val pager = wire[StudyPager]

  lazy val preview = wire[ChapterPreviewApi]

  lazy val pgnDump = wire[PgnDump]

  lazy val gifExport = GifExport(ws, appConfig.get[String]("game.gifUrl"))

  def findConnectedUsersIn(studyId: StudyId)(filter: Iterable[UserId] => Fu[List[UserId]]): Fu[List[UserId]] =
    studyRepo
      .membersById(studyId)
      .flatMap:
        _.map(_.members.keys)
          .filter(_.nonEmpty)
          .so: members =>
            filter(members).flatMap:
              _.parallel: streamer =>
                isConnected(studyId, streamer).dmap(_.option(streamer))
              .dmap(_.flatten)

  def cli: lila.common.Cli = new:
    def process = { case "study" :: "rank" :: "reset" :: Nil =>
      studyRepo.resetAllRanks.map: count =>
        s"$count done"
    }

  lila.common.Bus.subscribeFun("studyAnalysisProgress"):
    case lila.tree.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)

  lila.common.Bus.sub[lila.core.user.UserDelete]: del =>
    for
      studyIds <- studyRepo.deletePrivateByOwner(del.id)
      _        <- chapterRepo.deleteByStudyIds(studyIds)
      _        <- studyRepo.anonymizeAllOf(del.id)
      _        <- topicApi.userTopicsDelete(del.id)
    yield ()
