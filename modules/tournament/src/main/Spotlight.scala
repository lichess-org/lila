package lila.tournament

import lila.user.User

case class Spotlight(
    headline: String,
    description: String,
    homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
    iconFont: Option[String] = None,
    iconImg: Option[String] = None
)

object Spotlight {

  import Schedule.Freq._

  def select(tours: List[Tournament], user: Option[User], max: Int): List[Tournament] =
    user.fold(sort(tours) take max) { select(tours, _, max) }

  def select(tours: List[Tournament], user: User, max: Int): List[Tournament] =
    sort(tours.filter { select(_, user) }) take max

  private def sort(tours: List[Tournament]) =
    tours.sortBy { t =>
      -(t.schedule.??(_.freq.importance))
    }

  private def select(tour: Tournament, user: User): Boolean =
    !tour.isFinished &&
      tour.spotlight.fold(automatically(tour, user)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists { hours =>
      tour.startsAt.minusHours(hours).isBeforeNow
    }

  private def automatically(tour: Tournament, user: User): Boolean =
    tour.perfType ?? { pt =>
      tour.schedule ?? { sched =>
        def playedSinceWeeks(weeks: Int) =
          user.perfs(pt).latest ?? { l =>
            l.plusWeeks(weeks).isAfterNow
          }
        sched.freq match {
          case Hourly                               => playedSinceWeeks(4)
          case Daily | Eastern                      => true
          case Weekly | Weekend                     => true
          case Unique                               => true
          case Monthly | Shield | Marathon | Yearly => true
          case _                                    => false
        }
      }
    }
}
