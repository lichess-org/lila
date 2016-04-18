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
    system: ActorSystem,
    lightUser: String => Option[lila.common.LightUser],
    hub: lila.hub.Env,
    db: lila.db.Env) {

  private val settings = new {
    val CollectionStudy = config getString "collection.study"
    val HistoryMessageTtl = config duration "history.message.ttl"
    val UidTimeout = config duration "uid.timeout"
    val SocketTimeout = config duration "socket.timeout"
    val SocketName = config getString "socket.name"
    val SequencerTimeout = config duration "sequencer.timeout"
  }
  import settings._

  private val socketHub = system.actorOf(
    Props(new lila.socket.SocketHubActor.Default[Socket] {
      def mkActor(studyId: String) = new Socket(
        studyId = studyId,
        history = new lila.socket.History(ttl = HistoryMessageTtl),
        getStudy = repo.byId,
        uidTimeout = UidTimeout,
        socketTimeout = SocketTimeout)
    }), name = SocketName)

  def version(studyId: Study.ID): Fu[Int] =
    socketHub ? Ask(studyId, GetVersion) mapTo manifest[Int]

  lazy val socketHandler = new SocketHandler(
    hub = hub,
    socketHub = socketHub,
    api = api)

  lazy val jsonView = new JsonView(lightUser)

  lazy val api = new StudyApi(
    repo = repo,
    jsonView = jsonView,
    sequencers = sequencerMap,
    socketHub = socketHub)

  private val sequencerMap = system.actorOf(Props(ActorMap { id =>
    new Sequencer(
      receiveTimeout = SequencerTimeout.some,
      executionTimeout = 5.seconds.some,
      logger = logger)
  }))

  private lazy val repo = new StudyRepo(coll = db(CollectionStudy))
}

object Env {

  lazy val current: Env = "study" boot new Env(
    config = lila.common.PlayApp loadConfig "study",
    system = lila.common.PlayApp.system,
    lightUser = lila.user.Env.current.lightUser,
    hub = lila.hub.Env.current,
    db = lila.db.Env.current)
}
