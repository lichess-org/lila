package lila.relay

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.{ Adapter, CachedAdapter }
import lila.user.User

final class RelayPager(
    repo: RelayRepo,
    maxPerPage: lila.common.MaxPerPage
) {

  import BSONHandlers._

  def all(me: Option[User], order: Order, page: Int) = paginator(
    accessSelect(me), me, order, page, fuccess(9999).some
  )

  private def paginator(
    selector: Bdoc,
    me: Option[User],
    order: Order,
    page: Int,
    nbResults: Option[Fu[Int]] = none
  ): Fu[Paginator[Study.WithChaptersAndLiked]] = {
    val adapter = new Adapter[Study](
      collection = studyRepo.coll,
      selector = selector,
      projection = studyRepo.projection,
      sort = order match {
        case Order.Hot => $sort desc "rank"
        case Order.Newest => $sort desc "createdAt"
        case Order.Oldest => $sort asc "createdAt"
        case Order.Updated => $sort desc "updatedAt"
        case Order.Popular => $sort desc "likes"
      }
    ) mapFutureList withChaptersAndLiking(me)
    Paginator(
      adapter = nbResults.fold(adapter) { nb =>
        new CachedAdapter(adapter, nb)
      },
      currentPage = page,
      maxPerPage = maxPerPage.value
    )
  }

  def withChapters(studies: Seq[Study]): Fu[Seq[Study.WithChapters]] =
    chapterRepo idNamesByStudyIds studies.map(_.id) map { chapters =>
      studies.map { study =>
        Study.WithChapters(study, ~(chapters get study.id map {
          _ map (_.name)
        }))
      }
    }

  def withLiking(me: Option[User])(studies: Seq[Study.WithChapters]): Fu[Seq[Study.WithChaptersAndLiked]] =
    me.?? { u => studyRepo.filterLiked(u, studies.map(_.study.id)) } map { liked =>
      studies.map {
        case Study.WithChapters(study, chapters) =>
          Study.WithChaptersAndLiked(study, chapters, liked(study.id))
      }
    }

  def withChaptersAndLiking(me: Option[User])(studies: Seq[Study]): Fu[Seq[Study.WithChaptersAndLiked]] =
    withChapters(studies) flatMap withLiking(me)
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
  val allButOldest = all filter (Oldest !=)
  private val byKey: Map[String, Order] = all.map { o => o.key -> o }(scala.collection.breakOut)
  def apply(key: String): Order = byKey.getOrElse(key, default)
}
