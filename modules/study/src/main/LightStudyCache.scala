package lila.study

import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import scala.concurrent.duration._

import lila.study._
import lila.user.User

final class LightStudyCache(studyRepo: StudyRepo,
	                          asyncCache: lila.memo.AsyncCache.Builder) {
	private val cache = asyncCache.clearable(
	  name = "study.lightStudyCache",
    f = fetch,
    expireAfter = _.ExpireAfterWrite(20 minutes)
	)

    def remove(studyId: Study.Id): Unit =
      cache invalidate studyId

    def get(studyId: Study.Id): Fu[Option[LightStudy]] =
      cache get studyId

    private def fetch(studyId: Study.Id): Fu[Option[LightStudy]] =
      studyRepo byId studyId flatMap { studyOption =>
        studyOption match {
          case Some(study) => fuccess(Some(new LightStudy(study.isPublic, study.members.members.filter(_._2.canContribute).map(_._1).toSet)))
          case None => fuccess(None)
        }
      }
}


final class LightStudy(val isPublic: Boolean, val contributors: Set[User.ID])