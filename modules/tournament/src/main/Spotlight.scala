package lila.tournament

import lila.common.Heapsort.topN
import lila.user.User
import lila.common.licon

case class Spotlight(
    headline: String,
    description: String,
    homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
    iconFont: Option[licon.Icon] = None,
    iconImg: Option[String] = None
)

object Spotlight:

  import Schedule.Freq.*

  private given Ordering[Tournament] = Ordering.by[Tournament, Int](_.schedule.so(_.freq.importance))

  def select(tours: List[Tournament], user: Option[User], max: Int): List[Tournament] =
    user.fold(select(tours, max)) { select(tours, _, max) }

  def select(tours: List[Tournament], max: Int): List[Tournament] =
    tours filter { tour => tour.spotlight.fold(true) { manually(tour, _) } } topN max

  def select(tours: List[Tournament], user: User, max: Int): List[Tournament] =
    tours.filter { select(_, user) } topN max

  private def select(tour: Tournament, user: User): Boolean =
    !tour.isFinished &&
      tour.spotlight.fold(automatically(tour, user)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists { hours =>
      tour.startsAt.minusHours(hours).isBeforeNow
    }

  private def automatically(tour: Tournament, user: User): Boolean =
    tour.schedule so { sched =>
      def playedSinceWeeks(weeks: Int) =
        user.perfs(tour.perfType).latest so {
          _.plusWeeks(weeks).isAfterNow
        }
      sched.freq match
        case Hourly                               => canMaybeJoinLimited(tour, user) && playedSinceWeeks(2)
        case Daily | Eastern                      => playedSinceWeeks(2)
        case Weekly | Weekend                     => playedSinceWeeks(4)
        case Unique                               => playedSinceWeeks(4)
        case Monthly | Shield | Marathon | Yearly => true
        case ExperimentalMarathon                 => false
    }

  private def canMaybeJoinLimited(tour: Tournament, user: User): Boolean =
    tour.conditions.isRatingLimited &&
      tour.conditions.nbRatedGame.fold(true) { c =>
        c(user, tour.perfType).accepted
      } &&
      tour.conditions.minRating.fold(true) { c =>
        c(user, tour.perfType).accepted
      } &&
      tour.conditions.maxRating.fold(true)(_.maybe(user, tour.perfType))
