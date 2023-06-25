package lila.tournament

import lila.common.Heapsort.topN
import lila.user.Me
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

  def select(tours: List[Tournament], max: Int)(using me: Option[Me]): List[Tournament] =
    me.foldUse(select(tours))(selectForMe(tours)) topN max

  private def select(tours: List[Tournament]): List[Tournament] =
    tours filter { tour => tour.spotlight.fold(true) { manually(tour, _) } }

  private def selectForMe(tours: List[Tournament])(using Me): List[Tournament] =
    tours filter selectForMe

  private def selectForMe(tour: Tournament)(using Me): Boolean =
    !tour.isFinished &&
      tour.spotlight.fold(automatically(tour)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists: hours =>
      tour.startsAt.minusHours(hours).isBeforeNow

  private def automatically(tour: Tournament)(using me: Me): Boolean =
    tour.schedule.so: sched =>
      def playedSinceWeeks(weeks: Int) =
        me.perfs(tour.perfType).latest so {
          _.plusWeeks(weeks).isAfterNow
        }
      sched.freq match
        case Hourly                               => canMaybeJoinLimited(tour) && playedSinceWeeks(2)
        case Daily | Eastern                      => playedSinceWeeks(2)
        case Weekly | Weekend                     => playedSinceWeeks(4)
        case Unique                               => playedSinceWeeks(4)
        case Monthly | Shield | Marathon | Yearly => true
        case ExperimentalMarathon                 => false

  private def canMaybeJoinLimited(tour: Tournament)(using Me): Boolean =
    tour.conditions.isRatingLimited &&
      tour.conditions.nbRatedGame.fold(true) { c =>
        c(tour.perfType).accepted
      } &&
      tour.conditions.minRating.fold(true) { c =>
        c(tour.perfType).accepted
      } &&
      tour.conditions.maxRating.fold(true)(_.maybe(tour.perfType))
