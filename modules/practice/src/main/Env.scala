package lila.practice

import com.softwaremill.macwire._
import play.api.Configuration

import lila.common.config._

final class Env(
    appConfig: Configuration,
    configStoreApi: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Env
) {

  private lazy val coll = db(CollName("practice_progress"))

  import PracticeConfig.loader
  private lazy val configStore = configStoreApi[PracticeConfig]("practice", logger)

  lazy val api: PracticeApi = wire[PracticeApi]

  lila.common.Bus.subscribeFun("study") {
    case lila.study.actorApi.SaveStudy(study) => api.structure onSave study
  }
}
