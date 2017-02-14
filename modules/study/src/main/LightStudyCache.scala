package lila.study

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import scala.concurrent.duration._

import lila.study._
import lila.user.User

final class LightStudyCache(
    studyRepo: StudyRepo,
    asyncCache: lila.memo.AsyncCache.Builder) {

  private val cache = asyncCache.clearable(
    name = "study.lightStudyCache",
    f = fetch,
    expireAfter = _.ExpireAfterWrite(20 minutes))

  def remove(studyId: Study.Id): Unit =
    cache invalidate studyId

  def get(studyId: Study.Id): Fu[Option[LightStudy]] =
    cache get studyId

  private def fetch(studyId: Study.Id): Fu[Option[LightStudy]] =
    studyRepo byId studyId map2 { (s: Study) =>
      LightStudy(s.isPublic, s.members.contributorIds.toSet)
    }
}

case class LightStudy(isPublic: Boolean, contributors: Set[User.ID])
