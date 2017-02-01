package lila.practice

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    configStore: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    evalCache: lila.evalCache.EvalCacheApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env,
    hub: lila.hub.Env,
    system: ActorSystem) {

  private val CollectionProgress = config getString "collection.progress"
  private val SocketUidTtl = config duration "socket.uid.ttl"
  private val SocketName = config getString "socket.name"

  lazy val api = new PracticeApi(
    coll = db(CollectionProgress),
    configStore = configStore[PracticeConfig]("practice", logger),
    asyncCache = asyncCache,
    studyApi = studyApi)

  private val socket = system.actorOf(
    Props(new PracticeSocket(timeout = SocketUidTtl)), name = SocketName)

  lazy val socketHandler = new PracticeSocketHandler(socket, hub, evalCache)

  system.lilaBus.subscribe(system.actorOf(Props(new Actor {
    import lila.study.actorApi._
    def receive = {
      case SaveStudy(study) => api.structure onSave study
    }
  })), 'study)
}

object Env {

  lazy val current: Env = "practice" boot new Env(
    config = lila.common.PlayApp loadConfig "practice",
    configStore = lila.memo.Env.current.configStore,
    studyApi = lila.study.Env.current.api,
    evalCache = lila.evalCache.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current,
    hub = lila.hub.Env.current,
    system = lila.common.PlayApp.system)
}
