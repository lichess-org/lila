package lila.tournament

import lila.common.Heapsort.topN
import lila.user.{ User, Me }
import lila.common.licon
import lila.rating.Perf

case class Spotlight(
    headline: String,
    homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
    iconFont: Option[licon.Icon] = None,
    iconImg: Option[String] = None
)

object Spotlight:

  import Schedule.Freq.*

  private given Ordering[Tournament] = Ordering.by[Tournament, (Int, Int)]: tour =>
    tour.schedule match
      case Some(schedule) => (schedule.freq.importance, -tour.secondsToStart)
      case None           => (tour.isTeamRelated.so(Schedule.Freq.Weekly.importance), -tour.secondsToStart)

  def select(tours: List[Tournament], max: Int)(using me: Option[User.WithPerfs]): List[Tournament] =
    me.foldUse(select(tours))(selectForMe(tours)) topN max

  private def select(tours: List[Tournament]): List[Tournament] =
    tours.filter: tour =>
      tour.spotlight.forall(manually(tour, _))

  private def selectForMe(tours: List[Tournament])(using User.WithPerfs): List[Tournament] =
    tours filter selectForMe

  private def selectForMe(tour: Tournament)(using User.WithPerfs): Boolean =
    !tour.isFinished &&
      tour.spotlight.fold(automatically(tour)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists: hours =>
      tour.startsAt.minusHours(hours).isBeforeNow

  private def automatically(tour: Tournament)(using me: User.WithPerfs): Boolean =
    tour.isTeamRelated || tour.schedule.so: sched =>
      def playedSinceWeeks(weeks: Int) = me.perfs(tour.perfType).latest.so(_.plusWeeks(weeks).isAfterNow)
      sched.freq match
        case Hourly                               => canMaybeJoinLimited(tour) && playedSinceWeeks(2)
        case Daily | Eastern                      => playedSinceWeeks(2)
        case Weekly | Weekend                     => playedSinceWeeks(4)
        case Unique                               => playedSinceWeeks(4)
        case Monthly | Shield | Marathon | Yearly => true
        case ExperimentalMarathon                 => false

  private def canMaybeJoinLimited(tour: Tournament)(using me: User.WithPerfs): Boolean =
    given Me   = Me(me.user)
    given Perf = me.perfs(tour.perfType)
    tour.conditions.isRatingLimited &&
    tour.conditions.nbRatedGame.forall { c =>
      c(tour.perfType).accepted
    } &&
    tour.conditions.minRating.forall { c =>
      c(tour.perfType).accepted
    } &&
    tour.conditions.maxRating.forall(_.maybe(tour.perfType))
