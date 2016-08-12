package lila.tournament

import org.joda.time.DateTime

import lila.user.User

case class Spotlight(
  headline: String,
  description: String,
  homepageHours: Option[Int] = None, // feature on homepage hours before start (max 24)
  iconFont: Option[String] = None,
  iconImg: Option[String] = None)

object Spotlight {

  import Schedule.Freq._

  def select(tours: List[Tournament], user: Option[User]): List[Tournament] =
    user.fold(sort(tours) take 3) { select(tours, _) }

  def select(tours: List[Tournament], user: User): List[Tournament] =
    sort(tours.filter { select(_, user) }) take 3

  private def sort(tours: List[Tournament]) = tours.sortBy { t =>
    -(t.schedule.??(_.freq.importance))
  }

  private def select(tour: Tournament, user: User): Boolean = !tour.isFinished &&
    tour.spotlight.fold(automatically(tour, user)) { manually(tour, _) }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists { hours =>
      tour.startsAt.minusHours(hours) isBefore DateTime.now
    }

  private def automatically(tour: Tournament, user: User): Boolean = tour.perfType ?? { pt =>
    tour.schedule ?? { sched =>
      val perf = user.perfs(pt)
      def playedSinceWeeks(weeks: Int) = perf.latest ?? { l =>
        l.plusWeeks(weeks) isAfter DateTime.now
      }
      sched.freq match {
        case Hourly               => false
        case Daily | Eastern      => playedSinceWeeks(2)
        case Weekly               => playedSinceWeeks(4)
        case Unique               => playedSinceWeeks(4)
        case Monthly | Marathon   => true
        case ExperimentalMarathon => false
      }
    }
  }
}
