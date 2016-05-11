package lila.study

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo) {

  import BSONHandlers._
  import studyRepo.{ selectPublic, selectMemberId, selectOwnerId }

  def whereUidsContain(userId: User.ID, page: Int) = paginator($doc("uids" -> userId), page)

  def byOwner(ownerId: User.ID, page: Int) = paginator($doc("ownerId" -> ownerId), page)

  def byOwnerForUser(ownerId: User.ID, user: Option[User], page: Int) = paginator(
    selectOwnerId(ownerId) ++ accessSelect(user), page)

  def byMemberForUser(memberId: User.ID, user: Option[User], page: Int) = paginator(
    selectMemberId(memberId) ++ $doc("ownerId" $ne memberId) ++ accessSelect(user), page)

  def accessSelect(user: Option[User]) =
    user.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(selector: Bdoc, page: Int): Fu[Paginator[Study.WithChapters]] = Paginator(
    adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = $empty,
      sort = $sort desc "createdAt"
    ) mapFutureList withChapters,
    currentPage = page,
    maxPerPage = 14)

  private def withChapters(studies: Seq[Study]): Fu[Seq[Study.WithChapters]] =
    chapterRepo namesByStudyIds studies.map(_.id) map { chapters =>
      studies.map { study =>
        Study.WithChapters(study, ~(chapters get study.id))
      }
    }
}
