package lila.practice

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    studyApi: lila.study.StudyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db,
    memoConfig: lila.memo.MemoConfig
)(using Executor):

  private lazy val coll = db(CollName("practice_progress"))

  private lazy val configStoreBuilder = wire[ConfigStore.Builder]

  import PracticeConfig.given
  private lazy val configStore = configStoreBuilder[PracticeConfig]("practice", logger)

  lazy val api: PracticeApi = wire[PracticeApi]

  def getStudies: lila.core.practice.GetStudies = api.structure.getStudies

  lila.common.Bus.subscribeFun("study") { case lila.study.SaveStudy(study) =>
    api.structure.onSave(study)
  }
