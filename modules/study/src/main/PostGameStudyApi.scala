package lila.study

import scala.concurrent.duration._

import lila.user.User

final class PostGameStudyApi(
    cacheApi: lila.memo.CacheApi,
    studyApi: StudyApi
) {

  private val cache = cacheApi[String, Option[Study.Id]](64, "study.postGameStudyApi") {
    _.expireAfterWrite(10 minutes)
      .buildAsyncFuture(key => {
        val (gameId, users) = BSONHandlers.decodePostGameStudyKey(key)
        studyApi.postGameStudy(gameId, users).dmap2(_._id)
      })
  }

  def get(data: StudyForm.postGameStudy.Data, me: User): Fu[Option[Study.Id]] =
    cache.get(BSONHandlers.encodePostGameStudyKey(data.gameId, me.id :: data.users))
}
