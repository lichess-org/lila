package lila.coach

import play.api.i18n.Lang
import reactivemongo.api._

import lila.coach.CoachPager.Order.Alphabetical
import lila.coach.CoachPager.Order.LichessRating
import lila.coach.CoachPager.Order.Login
import lila.coach.CoachPager.Order.NbReview
import lila.common.paginator.{ AdapterLike, Paginator }
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.{ Country, User, UserMark, UserRepo }

final class CoachPager(
    userRepo: UserRepo,
    coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  val maxPerPage = lila.common.config.MaxPerPage(10)

  import CoachPager._
  import BsonHandlers._

  def apply(
      lang: Option[Lang],
      order: Order,
      country: Option[Country],
      page: Int
  ): Fu[Paginator[Coach.WithUser]] = {
    def selector = listableSelector ++ lang.?? { l => $doc("languages" -> l.code) }

    val adapter =
      new AdapterLike[Coach.WithUser] {
        def nbResults: Fu[Int] = coll.secondaryPreferred.countSel(selector)

        def slice(offset: Int, length: Int): Fu[List[Coach.WithUser]] =
          coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(selector) -> List(
                Sort(
                  order match {
                    case Alphabetical  => Ascending("_id")
                    case NbReview      => Descending("nbReview")
                    case LichessRating => Descending("user.rating")
                    case Login         => Descending("user.seenAt")
                  }
                ),
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from"         -> userRepo.coll.name,
                      "localField"   -> "_id",
                      "foreignField" -> "_id",
                      "as"           -> "_user"
                    )
                  )
                ),
                UnwindField("_user"),
                Match(
                  $doc(
                    s"_user.${User.BSONFields.marks}" $nin List(
                      UserMark.Engine.key,
                      UserMark.Boost.key,
                      UserMark.Troll.key
                    )
                  ) ++ country.?? { c =>
                    $doc("_user.profile.country" -> c.code)
                  }
                ),
                Skip(offset),
                Limit(length)
              )
            }
            .map { docs =>
              for {
                doc   <- docs
                coach <- doc.asOpt[Coach]
                user  <- doc.getAsOpt[User]("_user")
              } yield Coach.WithUser(coach, user)
            }
      }

    Paginator(
      adapter,
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private val listableSelector = $doc(
    "listed"    -> Coach.Listed(true),
    "available" -> Coach.Available(true)
  )

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
      val name: String
  )

  object Order {
    case object Login         extends Order("login", "Last login")
    case object LichessRating extends Order("rating", "Lichess rating")
    case object NbReview      extends Order("review", "User reviews")
    case object Alphabetical  extends Order("alphabetical", "Alphabetical")

    val default                   = Login
    val all                       = List(Login, LichessRating, NbReview, Alphabetical)
    def apply(key: String): Order = all.find(_.key == key) | default
  }
}
