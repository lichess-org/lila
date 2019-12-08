package lila.practice

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    configStoreApi: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    asyncCache: lila.memo.AsyncCache.Builder,
    db: lila.db.Db
) {

  private lazy val coll = db(CollName("practice_progress"))

  import PracticeConfig.loader
  private lazy val configStore = configStoreApi[PracticeConfig]("practice", logger)

  lazy val api: PracticeApi = wire[PracticeApi]

  lila.common.Bus.subscribeFun("study") {
    case lila.study.actorApi.SaveStudy(study) => api.structure onSave study
  }
}
