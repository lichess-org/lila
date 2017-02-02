package lila.practice

import akka.actor._
import com.typesafe.config.Config
import scala.concurrent.duration._

import lila.common.PimpedConfig._

final class Env(
    config: Config,
    configStore: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env,
    system: ActorSystem) {

  private val CollectionProgress = config getString "collection.progress"

  lazy val api = new PracticeApi(
    coll = db(CollectionProgress),
    configStore = configStore[PracticeConfig]("practice", logger),
    asyncCache = asyncCache,
    studyApi = studyApi)

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
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current,
    system = lila.common.PlayApp.system)
}
