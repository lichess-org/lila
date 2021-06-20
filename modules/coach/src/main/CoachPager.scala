package lila.coach

import reactivemongo.api._
import play.api.i18n.Lang

import lila.common.paginator.{ Paginator, AdapterLike }
import lila.db.dsl._
import lila.db.paginator.Adapter
import lila.user.{ User, UserRepo }
import lila.user.Country

final class CoachPager(
    userRepo: UserRepo,
    coachRepo: CoachRepo,
    coll: Coll
)(implicit ec: scala.concurrent.ExecutionContext) {

  val maxPerPage = lila.common.config.MaxPerPage(10)

  import CoachPager._
  import BsonHandlers._

  def apply(lang: Option[Lang], order: Order, country: Option[Country], page: Int): Fu[Paginator[Coach.WithUser]] = {
    Paginator(
      adapter = new AdapterLike[Coach.WithUser] {

        def nbResults: Fu[Int] = fuccess(9999)

        def slice(offset: Int, length: Int): Fu[List[Coach.WithUser]] =
          coachRepo.coll
            .aggregateList(length, readPreference = ReadPreference.secondaryPreferred) { framework =>
              import framework._
              Match(lang.?? { l => $doc("languages" -> l.code) }) -> List(
                PipelineOperator(
                  $doc(
                    "$lookup" -> $doc(
                      "from" -> userRepo.coll.name,
                      "localField" -> "_id",
                      "foreignField" -> "_id",
                      "as" -> "coach"
                    )
                  )
                ),
                UnwindField("coach"),
                Match(country.?? { c => $doc("coach.profile.country" -> c.code) })
              )
            }
            .map { docs =>
              for {
                doc <- docs
                coach <- doc.asOpt[Coach]
                user <- doc.getAsOpt[User]("coach")
              } yield Coach.WithUser(coach, user)
            }
      },
      currentPage = page,
      maxPerPage = maxPerPage
    )
  }

  private val listableSelector = $doc(
    "listed"    -> Coach.Listed(true),
    "approved"  -> Coach.Approved(true),
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
      val name: String,
      val predicate: Bdoc
  )

  object Order {
    case object Login         extends Order("login", "Last login", $sort desc "user.seenAt")
    case object LichessRating extends Order("rating", "Lichess rating", $sort desc "user.rating")
    case object NbReview      extends Order("review", "User reviews", $sort desc "nbReviews")
    case object Alphabetical  extends Order("alphabetical", "Alphabetical", $sort asc "_id")

    val default                   = Login
    val all                       = List(Login, LichessRating, NbReview, Alphabetical)
    def apply(key: String): Order = all.find(_.key == key) | default
  }
}
