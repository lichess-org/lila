package lila.practice

import com.softwaremill.macwire._

import lila.common.config._

@Module
final class Env(
    configStoreApi: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(implicit ec: scala.concurrent.ExecutionContext) {

  private lazy val coll = db(CollName("practice_progress"))

  import PracticeConfig.loader
  private lazy val configStore = configStoreApi[PracticeConfig]("practice", logger)

  lazy val api: PracticeApi = wire[PracticeApi]

  lila.common.Bus.subscribeFun("study") {
    case lila.study.actorApi.SaveStudy(study) => api.structure onSave study
  }
}
