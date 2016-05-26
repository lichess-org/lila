package lila.study

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.User

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo) {

  import BSONHandlers._
  import studyRepo.{ selectPublic, selectPrivate, selectMemberId, selectOwnerId, selectLiker }

  def byOwnerForUser(ownerId: User.ID, user: Option[User], page: Int) = paginator(
    selectOwnerId(ownerId) ++ accessSelect(user), page)

  def byOwnerPublicForUser(ownerId: User.ID, user: Option[User], page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPublic, page)

  def byOwnerPrivateForUser(ownerId: User.ID, user: Option[User], page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPrivate ++ accessSelect(user), page)

  def byMemberForUser(memberId: User.ID, user: Option[User], page: Int) = paginator(
    selectMemberId(memberId) ++ $doc("ownerId" $ne memberId) ++ accessSelect(user), page)

  def byLikesForUser(userId: User.ID, user: Option[User], page: Int) = paginator(
    selectLiker(userId) ++ accessSelect(user) ++ user.?? { u =>
      $doc("uids" $ne u.id)
    }, page)

  def accessSelect(user: Option[User]) =
    user.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(selector: Bdoc, page: Int): Fu[Paginator[Study.WithChapters]] = Paginator(
    adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = studyRepo.projection,
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
