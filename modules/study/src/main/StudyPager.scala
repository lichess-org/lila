package lila.study

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.user.User

final class StudyPager(
    studyRepo: StudyRepo,
    chapterRepo: ChapterRepo) {

  import BSONHandlers._
  import studyRepo.{ selectPublic, selectPrivate, selectMemberId, selectOwnerId, selectLiker }

  def all(me: Option[User], order: Order, page: Int) = paginator(
    accessSelect(me), me, order, page, fuccess(9999).some)

  def byOwner(owner: User, me: Option[User], order: Order, page: Int) = paginator(
    selectOwnerId(owner.id) ++ accessSelect(me), me, order, page)

  def mine(me: User, order: Order, page: Int) = paginator(
    selectOwnerId(me.id), me.some, order, page)

  def minePublic(me: User, order: Order, page: Int) = paginator(
    selectOwnerId(me.id) ++ selectPublic, me.some, order, page)

  def minePrivate(me: User, order: Order, page: Int) = paginator(
    selectOwnerId(me.id) ++ selectPrivate, me.some, order, page)

  def mineMember(me: User, order: Order, page: Int) = paginator(
    selectMemberId(me.id) ++ $doc("ownerId" $ne me.id), me.some, order, page)

  def mineLikes(me: User, order: Order, page: Int) = paginator(
    selectLiker(me.id) ++ accessSelect(me.some) ++ $doc("ownerId" $ne me.id), me.some, order, page)

  def accessSelect(me: Option[User]) =
    me.fold(selectPublic) { u =>
      $or(selectPublic, selectMemberId(u.id))
    }

  private def paginator(
    selector: Bdoc,
    me: Option[User],
    order: Order,
    page: Int,
    nbResults: Option[Fu[Int]] = none): Fu[Paginator[Study.WithChaptersAndLiked]] = {
    val adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = studyRepo.projection,
      sort = order match {
        case Order.Hot     => $sort desc "rank"
        case Order.Newest  => $sort desc "createdAt"
        case Order.Oldest  => $sort asc "createdAt"
        case Order.Updated => $sort desc "updatedAt"
        case Order.Popular => $sort desc "likes"
      }
    ) mapFutureList withChapters mapFutureList withLiking(me)
    Paginator(
      adapter = nbResults.fold(adapter) { nb =>
        new CachedAdapter(adapter, nb)
      },
      currentPage = page,
      maxPerPage = 14)
  }

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
  case object Hot extends Order("hot", "Hot")
  case object Newest extends Order("newest", "Date added (newest)")
  case object Oldest extends Order("oldest", "Date added (oldest)")
  case object Updated extends Order("updated", "Recently updated")
  case object Popular extends Order("popular", "Most popular")

  val default = Hot
  val all = List(Hot, Newest, Oldest, Updated, Popular)
  def apply(key: String): Order = all.find(_.key == key) | default
}
