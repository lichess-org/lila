package lila.study

import akka.actor._
import akka.pattern.ask
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._
import lila.hub.actorApi.map.Ask
import lila.hub.{ ActorMap, Sequencer }
import lila.socket.actorApi.GetVersion
import makeTimeout.short

final class Env(
    config: Config,
    getLightUser: lila.common.LightUser.Getter,
    gamePgnDump: lila.game.PgnDump,
    importer: lila.importer.Importer,
    system: ActorSystem,
    hub: lila.hub.Env,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val CollectionChapter = config getString "collection.chapter"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val SequencerTimeout = config duration "sequencer.timeout"
    val NetDomain = config getString "net.domain"
    val NetBaseUrl = config getString "net.base_url"
  }
  import settings._

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(studyId: String) = new Socket(
        studyId = studyId,
        jsonView = jsonView,
        studyRepo = studyRepo,
        lightUser = getLightUser,
        history = new lila.socket.History(ttl = HistoryMessageTtl),
        destCache = destCache,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout)
    }), name = SocketName)

  def version(studyId: Study.ID): Fu[Int] =
    socketHub ? Ask(studyId, GetVersion) mapTo manifest[Int]

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    chat = hub.actor.chat,
    destCache = destCache,
    api = api)

  lazy val studyRepo = new StudyRepo(coll = db(CollectionStudy))
  lazy val chapterRepo = new ChapterRepo(coll = db(CollectionChapter))

  lazy val jsonView = new JsonView(studyRepo, getLightUser, gamePgnDump)

  private lazy val chapterMaker = new ChapterMaker(
    importer = importer,
    lightUser = getLightUser,
    domain = NetDomain)

  private lazy val studyMaker = new StudyMaker(
    lightUser = getLightUser,
    chapterMaker = chapterMaker)

  lazy val api = new StudyApi(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo,
    sequencers = sequencerMap,
    chapterMaker = chapterMaker,
    studyMaker = studyMaker,
    notifier = new StudyNotifier(
      notifyApi = lila.notify.Env.current.notifyApi
    ),
    lightUser = getLightUser,
    chat = hub.actor.chat,
    timeline = hub.actor.timeline,
    socketHub = socketHub)

  lazy val pager = new StudyPager(
    studyRepo = studyRepo,
    chapterRepo = chapterRepo)

  lazy val pgnDump = new PgnDump(
    chapterRepo = chapterRepo,
    gamePgnDump = gamePgnDump,
    lightUser = getLightUser,
    netBaseUrl = NetBaseUrl)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(
      receiveTimeout = SequencerTimeout.some,
      executionTimeout = 5.seconds.some,
      logger = logger)
  }))

  private lazy val destCache = {
    import lila.socket.AnaDests
    lila.memo.Builder.cache[AnaDests.Ref, AnaDests](1 minute, _.compute)
  }
}

object Env {

  lazy val current: Env = "study" boot new Env(
    config = lila.common.PlayApp loadConfig "study",
    getLightUser = lila.user.Env.current.lightUser,
    gamePgnDump = lila.game.Env.current.pgnDump,
    importer = lila.importer.Env.current.importer,
    system = lila.common.PlayApp.system,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current)
}
