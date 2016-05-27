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

  def byOwnerForUser(ownerId: User.ID, me: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ accessSelect(me), me, order, page)

  def byOwnerPublicForUser(ownerId: User.ID, me: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPublic, me, order, page)

  def byOwnerPrivateForUser(ownerId: User.ID, me: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(ownerId) ++ selectPrivate ++ accessSelect(me), me, order, page)

  def byMemberForUser(memberId: User.ID, me: Option[User], order: Order, page: Int) = paginator(
    selectMemberId(memberId) ++ $doc("ownerId" $ne memberId) ++ accessSelect(me), me, order, page)

  def byLikesForUser(userId: User.ID, me: Option[User], order: Order, page: Int) = paginator(
    selectLiker(userId) ++ accessSelect(me) ++ $doc("ownerId" $ne userId), me, order, page)

  def accessSelect(me: Option[User]) =
    me.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(selector: Bdoc, me: Option[User], order: Order, page: Int): Fu[Paginator[Study.WithChaptersAndLiked]] = Paginator(
    adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = studyRepo.projection,
      sort = order match {
        case Order.Newest  => $sort desc "createdAt"
        case Order.Oldest  => $sort asc "createdAt"
        case Order.Popular => $sort desc "likes"
      }
    ) mapFutureList withChapters mapFutureList withLiking(me),
    currentPage = page,
    maxPerPage = 14)

  private def withChapters(studies: Seq[Study]): Fu[Seq[Study.WithChapters]] =
    chapterRepo namesByStudyIds studies.map(_.id) map { chapters =>
      studies.map { study =>
        Study.WithChapters(study, ~(chapters get study.id))
      }
    }

  private def withLiking(me: Option[User])(studies: Seq[Study.WithChapters]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.?? { u => studyRepo.filterLiked(u, studies.map(_.study.id)) } map { liked =>
      studies.map {
        case Study.WithChapters(study, chapters) =>
          Study.WithChaptersAndLiked(study, chapters, liked(study.id))
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
