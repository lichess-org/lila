package lila.practice

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    configStoreApi: lila.memo.ConfigStore.Builder,
    studyApi: lila.study.StudyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(using Executor):

  private lazy val coll = db(CollName("practice_progress"))

  import PracticeConfig.given
  private lazy val configStore = configStoreApi[PracticeConfig]("practice", logger)

  lazy val api: PracticeApi = wire[PracticeApi]

  def getStudies: lila.core.practice.GetStudies = api.structure.getStudies

  lila.common.Bus.subscribeFun("study") { case lila.study.actorApi.SaveStudy(study) =>
    api.structure.onSave(study)
  }
