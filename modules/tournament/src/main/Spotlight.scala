package lila.tournament

import lila.user.User

import org.joda.time.DateTime

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
    sort(filter(tours, user)) take max

  private def sort(tours: List[Tournament]) =
    tours.sortBy { t =>
      -t.nbPlayers
    }

  private def filter(tours: List[Tournament], user: Option[User]) =
    tours.filter { t =>
      !t.isFinished && user.fold(true)(validVariant(t, _)) && t.spotlight.fold(
        automatically(t, user.isDefined)
      )(
        manually(t, _)
      )
    }

  private def manually(tour: Tournament, spotlight: Spotlight): Boolean =
    spotlight.homepageHours.exists { hours =>
      tour.startsAt.minusHours(hours).isBeforeNow
    }

  private def automatically(tour: Tournament, isAnon: Boolean): Boolean =
    tour.schedule.fold(!isAnon && tour.popular) { schedule =>
      tour.startsAt isBefore DateTime.now.plusMinutes {
        schedule.freq match {
          case Unique            => 5 * 24 * 60
          case Yearly | Marathon => 3 * 24 * 60
          case Monthly | Shield  => 36 * 60
          case Weekly | Weekend  => 6 * 60
          case Daily             => 2 * 60
          case _                 => 30
        }
      }
    }

  private def validVariant(tour: Tournament, user: User): Boolean =
    tour.variant.standard || tour.perfType ?? { pt =>
      user.perfs(pt).latest ?? { l =>
        l.plusWeeks(4).isAfterNow
      }
    }

}
