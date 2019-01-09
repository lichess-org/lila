package lila.study

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.hub.actorApi.socket.HasUserId
import lila.hub.{ Duct, DuctMap }
import lila.socket.Socket.{ GetVersion, SocketVersion }
import lila.user.User

final class Env(
    config: Config,
    lightUserApi: lila.user.LightUserApi,
    gamePgnDump: lila.game.PgnDump,
    divider: lila.game.Divider,
    importer: lila.importer.Importer,
    explorerImporter: lila.explorer.ExplorerImporter,
    evalCacheHandler: lila.evalCache.EvalCacheSocketHandler,
    notifyApi: lila.notify.NotifyApi,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User.ID, User.ID) => Fu[Option[lila.relation.Relation]],
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val CollectionChapter = config getString "collection.chapter"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
    val NetBaseUrl = config getString "net.base_url"
    val MaxPerPage = config getInt "paginator.max_per_page"
  }
  import settings._

  val socketMap: SocketMap = lila.socket.SocketMap[StudySocket](
    system = system,
    mkTrouper = (studyId: String) => new StudySocket(
      system = system,
      studyId = Study.Id(studyId),
      jsonView = jsonView,
      studyRepo = studyRepo,
      chapterRepo = chapterRepo,
      lightUserApi = lightUserApi,
      history = new lila.socket.History(ttl = HistoryMessageTtl),
      uidTtl = UidTimeout,
      lightStudyCache = lightStudyCache,
      keepMeAlive = () => socketMap touch studyId
    ),
    accessTimeout = SocketTimeout,
    monitoringName = "study.socketMap",
    broomFrequency = 3697 millis
  )

  def version(studyId: Study.Id): Fu[SocketVersion] =
    socketMap.askIfPresentOrZero[SocketVersion](studyId.value)(GetVersion)

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socketMap.askIfPresentOrZero[Boolean](studyId.value)(HasUserId(userId, _))

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketMap = socketMap,
    chat = hub.chat,
    api = api,
    evalCacheHandler = evalCacheHandler
  )

  private lazy val chapterColl = db(CollectionChapter)

  lazy val studyRepo = new StudyRepo(coll = db(CollectionStudy))
  lazy val chapterRepo = new ChapterRepo(coll = chapterColl)

  lazy val jsonView = new JsonView(
    studyRepo,
    lightUserApi.sync
  )

  private lazy val chapterMaker = new ChapterMaker(
    importer = importer,
    pgnFetch = new PgnFetch,
    lightUser = lightUserApi,
    chat = hub.chat,
    domain = NetDomain
  )

  private lazy val explorerGame = new ExplorerGame(
    importer = explorerImporter,
    lightUser = lightUserApi.sync,
    baseUrl = NetBaseUrl
  )

  private lazy val studyMaker = new StudyMaker(
    lightUser = lightUserApi.sync,
    chapterMaker = chapterMaker
  )

  private lazy val studyInvite = new StudyInvite(
    studyRepo = studyRepo,
    notifyApi = notifyApi,
    getRelation = getRelation,
    getPref = getPref
  )

  private lazy val sequencer = new StudySequencer(
    studyRepo,
    chapterRepo,
    sequencers = new DuctMap(
      mkDuct = _ => Duct.extra.lazyPromise(5.seconds.some)(system),
      accessTimeout = SequencerTimeout
    )
  )

  private lazy val serverEvalRequester = new ServerEval.Requester(
    fishnetActor = hub.fishnet,
    chapterRepo = chapterRepo
  )

  lazy val serverEvalMerger = new ServerEval.Merger(
    sequencer = sequencer,
    api = api,
    chapterRepo = chapterRepo,
    socketMap = socketMap,
    divider = divider
  )

  lazy val api = new StudyApi(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    sequencer = sequencer,
    chapterMaker = chapterMaker,
    studyMaker = studyMaker,
    inviter = studyInvite,
    tagsFixer = new ChapterTagsFixer(chapterRepo, gamePgnDump),
    explorerGameHandler = explorerGame,
    lightUser = lightUserApi.sync,
    scheduler = system.scheduler,
    chat = hub.chat,
    bus = system.lilaBus,
    timeline = hub.timeline,
    socketMap = socketMap,
    serverEvalRequester = serverEvalRequester,
    lightStudyCache = lightStudyCache
  )

  lazy val pager = new StudyPager(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    maxPerPage = lila.common.MaxPerPage(MaxPerPage)
  )

  lazy val multiBoard = new StudyMultiBoard(
    runCommand = db.runCommand,
    chapterColl = chapterColl,
    maxPerPage = lila.common.MaxPerPage(9)
  )

  lazy val pgnDump = new PgnDump(
    chapterRepo = chapterRepo,
    gamePgnDump = gamePgnDump,
    lightUser = lightUserApi.sync,
    netBaseUrl = NetBaseUrl
  )

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

  system.lilaBus.subscribeFun('gdprErase, 'deploy) {
    case lila.user.User.GDPRErase(user) => api erase user
    case m: lila.hub.actorApi.Deploy => socketMap tellAll m
  }
  system.lilaBus.subscribeFun('studyAnalysisProgress) {
    case lila.analyse.actorApi.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)
  }
}

object Env {

  lazy val current: Env = "study" boot new Env(
    config = lila.common.PlayApp loadConfig "study",
    lightUserApi = lila.user.Env.current.lightUserApi,
    gamePgnDump = lila.game.Env.current.pgnDump,
    divider = lila.game.Env.current.divider,
    importer = lila.importer.Env.current.importer,
    explorerImporter = lila.explorer.Env.current.importer,
    evalCacheHandler = lila.evalCache.Env.current.socketHandler,
    notifyApi = lila.notify.Env.current.api,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
