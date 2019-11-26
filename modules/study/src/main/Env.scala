package lila.study

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

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
    notifyApi: lila.notify.NotifyApi,
    getPref: User => Fu[lila.pref.Pref],
    getRelation: (User.ID, User.ID) => Fu[Option[lila.relation.Relation]],
    remoteSocketApi: lila.socket.RemoteSocket,
    system: ActorSystem,
    hub: lila.hub.Env,
    chatApi: lila.chat.ChatApi,
    db: lila.db.Env,
    asyncCache: lila.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val CollectionChapter = config getString "collection.chapter"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
    val NetBaseUrl = config getString "net.base_url"
    val MaxPerPage = config getInt "paginator.max_per_page"
  }
  import settings._

  def version(studyId: Study.Id): Fu[SocketVersion] =
    socket.rooms.ask[SocketVersion](studyId.value)(GetVersion)

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socket.isPresent(studyId, userId)

  private val socket = new StudySocket(
    api = api,
    jsonView = jsonView,
    lightStudyCache = lightStudyCache,
    remoteSocketApi = remoteSocketApi,
    chatApi = chatApi
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
    pgnDump = gamePgnDump,
    lightUser = lightUserApi,
    chatApi = chatApi,
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
    socket = socket,
    divider = divider
  )

  lazy val api = new StudyApi(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    sequencer = sequencer,
    chapterMaker = chapterMaker,
    studyMaker = studyMaker,
    inviter = studyInvite,
    explorerGameHandler = explorerGame,
    lightUser = lightUserApi.sync,
    scheduler = system.scheduler,
    chatApi = chatApi,
    timeline = hub.timeline,
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

  lila.common.Bus.subscribeFun('gdprErase, 'studyAnalysisProgress) {
    case lila.user.User.GDPRErase(user) => api erase user
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
    notifyApi = lila.notify.Env.current.api,
    getPref = lila.pref.Env.current.api.getPref,
    getRelation = lila.relation.Env.current.api.fetchRelation,
    remoteSocketApi = lila.socket.Env.current.remoteSocket,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    chatApi = lila.chat.Env.current.api,
    db = lila.db.Env.current,
    asyncCache = lila.memo.Env.current.asyncCache
  )
}
