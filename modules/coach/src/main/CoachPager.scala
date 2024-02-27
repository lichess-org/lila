package lila.coach

import reactivemongo.api._
import play.api.i18n.Lang

import lila.common.paginator.Paginator
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.{ User, UserRepo }

final class CoachPager(
    userRepo: UserRepo,
    coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  val maxPerPage = lila.common.config.MaxPerPage(10)

  import CoachPager._
  import BsonHandlers._

  def apply(lang: Option[Lang], order: Order, page: Int): Fu[Paginator[Coach.WithUser]] = {
    val adapter = new Adapter[Coach](
      collection = coll,
      selector = $doc(
        "listed"   -> Coach.Listed(true),
        "approved" -> Coach.Approved(true)
      ) ++ lang.?? { l =>
        $doc("languages" -> l.code)
      },
      projection = none,
      sort = order.predicate
    ) mapFutureList withUsers
    Paginator(
      adapter = adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private def withUsers(coaches: Seq[Coach]): Fu[Seq[Coach.WithUser]] =
    userRepo.withColl {
      _.optionsByOrderedIds[User, User.ID](coaches.map(_.id.value), none, ReadPreference.secondaryPreferred)(
        _.id
      )
    } map { users =>
      coaches zip users collect { case (coach, Some(user)) =>
        Coach.WithUser(coach, user)
      }
    }
}

object CoachPager {

  sealed abstract class Order(
      val key: String,
      val name: String,
      val predicate: Bdoc
  )

  object Order {
    case object Login         extends Order("login", "Last login", $sort desc "user.seenAt")
    case object LishogiRating extends Order("rating", "Lishogi rating", $sort desc "user.rating")
    case object Alphabetical  extends Order("alphabetical", "Alphabetical", $sort asc "_id")

    val default                   = Login
    val all                       = List(Login, LishogiRating, Alphabetical)
    def apply(key: String): Order = all.find(_.key == key) | default
  }
}
