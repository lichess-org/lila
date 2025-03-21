package lila.tournament

import scalalib.HeapSort.topN

import lila.core.LightUser
import lila.core.perf.UserWithPerfs
import lila.ui.Icon

case class Spotlight(
    headline: String,
    homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
    iconFont: Option[Icon] = None,
    iconImg: Option[String] = None
)

object Spotlight:

  import Schedule.Freq.*

  private given Ordering[Tournament] = Ordering.by[Tournament, (Int, Int)]: tour =>
    tour.scheduleFreq match
      case Some(freq) => (freq.importance, -tour.secondsToStart.value)
      case None       => (tour.isTeamRelated.so(Schedule.Freq.Weekly.importance), -tour.secondsToStart.value)

  def select(tours: List[Tournament], max: Int)(using me: Option[UserWithPerfs]): List[Tournament] =
    me.foldUse(select(tours))(tours.filter(selectForMe).distinct).topN(max)

  private def select(tours: List[Tournament]): List[Tournament] =
    tours.filter: tour =>
      tour.spotlight.forall(manually(tour, _))

  private def selectForMe(tour: Tournament)(using UserWithPerfs): Boolean =
    !tour.isFinished &&
      tour.spotlight.fold(automatically(tour)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists: hours =>
      tour.startsAt.minusHours(hours).isBeforeNow

  private def automatically(tour: Tournament)(using me: UserWithPerfs): Boolean =
    tour.isTeamRelated || tour.scheduleFreq.so: freq =>
      def playedSinceWeeks(weeks: Int) = me.perfs(tour.perfType).latest.so(_.plusWeeks(weeks).isAfterNow)
      freq match
        case Hourly                               => canMaybeJoinLimited(tour) && playedSinceWeeks(2)
        case Daily | Eastern                      => playedSinceWeeks(2)
        case Weekly | Weekend                     => playedSinceWeeks(4)
        case Unique                               => playedSinceWeeks(4)
        case Monthly | Shield | Marathon | Yearly => true
        case ExperimentalMarathon                 => false

  private def canMaybeJoinLimited(tour: Tournament)(using me: UserWithPerfs): Boolean =
    given LightUser.Me = LightUser.Me(me.user.light)
    given Perf         = me.perfs(tour.perfType)
    tour.conditions.isRatingLimited &&
    tour.conditions.nbRatedGame.forall { c =>
      c(tour.perfType).accepted
    } &&
    tour.conditions.minRating.forall { c =>
      c(tour.perfType).accepted
    } &&
    tour.conditions.maxRating.forall(_.maybe(tour.perfType))
