package lila.practice

import com.softwaremill.macwire.*

import lila.core.config.*

@Module
final class Env(
    studyApi: lila.study.StudyApi,
    cacheApi: lila.memo.CacheApi,
    db: lila.db.Db
)(using Executor):

  private lazy val coll = db(CollName("practice_progress"))

  lazy val api: PracticeApi = wire[PracticeApi]

  def getStudies: lila.core.practice.GetStudies = api.structure.getStudies
