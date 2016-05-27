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

  def byOwnerForUser(ownerId: User.ID, user: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ accessSelect(user), order, page)

  def byOwnerPublicForUser(ownerId: User.ID, user: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPublic, order, page)

  def byOwnerPrivateForUser(ownerId: User.ID, user: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPrivate ++ accessSelect(user), order, page)

  def byMemberForUser(memberId: User.ID, user: Option[User], order: Order, page: Int) = paginator(
    selectMemberId(memberId) ++ $doc("ownerId" $ne memberId) ++ accessSelect(user), order, page)

  def byLikesForUser(userId: User.ID, user: Option[User], order: Order, page: Int) = paginator(
    selectLiker(userId) ++ accessSelect(user) ++ $doc("ownerId" $ne userId), order, page)

  def accessSelect(user: Option[User]) =
    user.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(selector: Bdoc, order: Order, page: Int): Fu[Paginator[Study.WithChapters]] = Paginator(
    adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = studyRepo.projection,
      sort = order match {
        case Order.Newest  => $sort desc "createdAt"
        case Order.Oldest  => $sort asc "createdAt"
        case Order.Popular => $sort desc "likes"
      }
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

sealed abstract class Order(val key: String, val name: String)

object Order {
  case object Newest extends Order("newest", "Date added (newest)")
  case object Oldest extends Order("oldest", "Date added (oldest)")
  case object Popular extends Order("popular", "Most popular")

  val default = Newest
  val all = List(Newest, Oldest, Popular)
  def apply(key: String): Order = all.find(_.key == key) | default
}
