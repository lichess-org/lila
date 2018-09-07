package lidraughts.study

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lidraughts.hub.actorApi.HasUserId
import lidraughts.hub.actorApi.map.Ask
import lidraughts.hub.{ ActorMap, Sequencer }
import lidraughts.socket.actorApi.GetVersion
import lidraughts.user.User
import makeTimeout.short

final class Env(
    config: Config,
    lightUserApi: lidraughts.user.LightUserApi,
    gamePdnDump: lidraughts.game.PdnDump,
    divider: lidraughts.game.Divider,
    importer: lidraughts.importer.Importer,
    explorerImporter: lidraughts.explorer.ExplorerImporter,
    evalCacheHandler: lidraughts.evalCache.EvalCacheSocketHandler,
    notifyApi: lidraughts.notify.NotifyApi,
    getPref: User => Fu[lidraughts.pref.Pref],
    getRelation: (User.ID, User.ID) => Fu[Option[lidraughts.relation.Relation]],
    system: ActorSystem,
    hub: lidraughts.hub.Env,
    db: lidraughts.db.Env,
    asyncCache: lidraughts.memo.AsyncCache.Builder
) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val CollectionChapter = config getString "collection.chapter"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val ActorName = config getString "actor.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
    val NetBaseUrl = config getString "net.base_url"
    val MaxPerPage = config getInt "paginator.max_per_page"
  }
  import settings._

  private val socketHub = system.actorOf(
    Props(new lidraughts.socket.SocketHubActor.Default[Socket] {
      def mkActor(studyId: String) = new Socket(
        studyId = Study.Id(studyId),
        jsonView = jsonView,
        studyRepo = studyRepo,
        chapterRepo = chapterRepo,
        lightUser = lightUserApi.async,
        history = new lidraughts.socket.History(ttl = HistoryMessageTtl),
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout,
        lightStudyCache = lightStudyCache
      )
    }), name = SocketName
  )

  def version(studyId: Study.Id): Fu[Int] =
    socketHub ? Ask(studyId.value, GetVersion) mapTo manifest[Int]

  def isConnected(studyId: Study.Id, userId: User.ID): Fu[Boolean] =
    socketHub ? Ask(studyId.value, HasUserId(userId)) mapTo manifest[Boolean]

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    api = api,
    evalCacheHandler = evalCacheHandler
  )

  lazy val studyRepo = new StudyRepo(coll = db(CollectionStudy))
  lazy val chapterRepo = new ChapterRepo(coll = db(CollectionChapter))

  lazy val jsonView = new JsonView(
    studyRepo,
    lightUserApi.sync
  )

  private lazy val chapterMaker = new ChapterMaker(
    importer = importer,
    pdnFetch = new PdnFetch,
    lightUser = lightUserApi,
    chat = hub.actor.chat,
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
    system.actorOf(Props(ActorMap { id =>
      new Sequencer(
        receiveTimeout = SequencerTimeout.some,
        executionTimeout = 5.seconds.some,
        logger = logger
      )
    }))
  )

  private lazy val serverEvalRequester = new ServerEval.Requester(
    fishnetActor = hub.actor.fishnet,
    chapterRepo = chapterRepo
  )

  lazy val serverEvalMerger = new ServerEval.Merger(
    sequencer = sequencer,
    api = api,
    chapterRepo = chapterRepo,
    socketHub = socketHub,
    divider = divider
  )

  // study actor
  system.actorOf(Props(new Actor {
    def receive = {
      case lidraughts.analyse.actorApi.StudyAnalysisProgress(analysis, complete) => serverEvalMerger(analysis, complete)
    }
  }), name = ActorName)

  lazy val api = new StudyApi(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    sequencer = sequencer,
    chapterMaker = chapterMaker,
    studyMaker = studyMaker,
    inviter = studyInvite,
    tagsFixer = new ChapterTagsFixer(chapterRepo, gamePdnDump),
    explorerGameHandler = explorerGame,
    lightUser = lightUserApi.sync,
    scheduler = system.scheduler,
    chat = hub.actor.chat,
    bus = system.lidraughtsBus,
    timeline = hub.actor.timeline,
    socketHub = socketHub,
    serverEvalRequester = serverEvalRequester,
    lightStudyCache = lightStudyCache
  )

  lazy val pager = new StudyPager(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    maxPerPage = lidraughts.common.MaxPerPage(MaxPerPage)
  )

  lazy val pdnDump = new PdnDump(
    chapterRepo = chapterRepo,
    gamePdnDump = gamePdnDump,
    lightUser = lightUserApi.sync,
    netBaseUrl = NetBaseUrl
  )

  lazy val lightStudyCache: LightStudyCache = asyncCache.multi(
    name = "study.lightStudyCache",
    f = studyRepo.lightById,
    expireAfter = _.ExpireAfterWrite(20 minutes)
  )

  def cli = new lidraughts.common.Cli {
    def process = {
      case "study" :: "rank" :: "reset" :: Nil => api.resetAllRanks.map { count => s"$count done" }
    }
  }
}

object Env {

  lazy val current: Env = "study" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "study",
    lightUserApi = lidraughts.user.Env.current.lightUserApi,
    gamePdnDump = lidraughts.game.Env.current.pdnDump,
    divider = lidraughts.game.Env.current.divider,
    importer = lidraughts.importer.Env.current.importer,
    explorerImporter = lidraughts.explorer.Env.current.importer,
    evalCacheHandler = lidraughts.evalCache.Env.current.socketHandler,
    notifyApi = lidraughts.notify.Env.current.api,
    getPref = lidraughts.pref.Env.current.api.getPref,
    getRelation = lidraughts.relation.Env.current.api.fetchRelation,
    system = lidraughts.common.PlayApp.system,
    hub = lidraughts.hub.Env.current,
    db = lidraughts.db.Env.current,
    asyncCache = lidraughts.memo.Env.current.asyncCache
  )
}
