package lila.practice

import scala.concurrent.duration._
import com.typesafe.config.Config

final class Env(
    config: Config,
    configStore: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    asyncCache: lila.memo.AsyncCache2.Builder,
    db: lila.db.Env) {

  private val CollectionProgress = config getString "collection.progress"

  lazy val api = new PracticeApi(
    coll = db(CollectionProgress),
    configStore = configStore[PracticeConfig]("practice", logger),
    asyncCache = asyncCache,
    studyApi = studyApi)
}

object Env {

  lazy val current: Env = "practice" boot new Env(
    config = lila.common.PlayApp loadConfig "practice",
    configStore = lila.memo.Env.current.configStore,
    studyApi = lila.study.Env.current.api,
    asyncCache = lila.memo.Env.current.asyncCache,
    db = lila.db.Env.current)
}
