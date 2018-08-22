package lidraughts.practice

import akka.actor._
import com.typesafe.config.Config

final class Env(
    config: Config,
    configStore: lidraughts.memo.ConfigStore.Builder,
    studyApi: lidraughts.study.StudyApi,
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    db: lidraughts.db.Env,
    system: ActorSystem
) {

  private val CollectionProgress = config getString "collection.progress"

  lazy val api = new PracticeApi(
    coll = db(CollectionProgress),
    configStore = configStore[PracticeConfig]("practice", logger),
    asyncCache = asyncCache,
    studyApi = studyApi,
    bus = system.lidraughtsBus
  )

  system.lidraughtsBus.subscribeFun('study) {
    case lidraughts.study.actorApi.SaveStudy(study) => api.structure onSave study
  }
}

object Env {

  lazy val current: Env = "practice" boot new Env(
    config = lidraughts.common.PlayApp loadConfig "practice",
    configStore = lidraughts.memo.Env.current.configStore,
    studyApi = lidraughts.study.Env.current.api,
    asyncCache = lidraughts.memo.Env.current.asyncCache,
    db = lidraughts.db.Env.current,
    system = lidraughts.common.PlayApp.system
  )
}
