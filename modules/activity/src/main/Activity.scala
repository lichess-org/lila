package lila.activity

import org.joda.time.{ DateTime, Days, Interval }

import lila.user.User

import activities._

case class Activity(
    id: Activity.Id,
    games: Option[Games] = None,
    posts: Option[Posts] = None,
    puzzles: Option[Puzzles] = None,
    learn: Option[Learn] = None,
    practice: Option[Practice] = None,
    simuls: Option[Simuls] = None,
    corres: Option[Corres] = None,
    patron: Option[Patron] = None,
    follows: Option[Follows] = None,
    studies: Option[Studies] = None,
    teams: Option[Teams] = None,
    stream: Boolean = false
) {

  def date = Activity.Day.genesis plusDays id.day.value

  def interval = new Interval(date, date plusDays 1)

  def isEmpty =
    !stream && List(games, posts, puzzles, learn, practice, simuls, corres, patron, follows, studies, teams)
      .forall(_.isEmpty)
}

object Activity {

  case class Id(userId: User.ID, day: Day)
  object Id {
    def today(userId: User.ID) = Id(userId, Day.today)
  }

  case class WithUserId(activity: Activity, userId: User.ID)

  // number of days since lichess
  case class Day(value: Int) extends AnyVal
  object Day {
    val genesis = new DateTime(2010, 1, 1, 0, 0).withTimeAtStartOfDay
    def today   = Day(Days.daysBetween(genesis, DateTime.now.withTimeAtStartOfDay).getDays)
    def recent(nb: Int): List[Day] =
      (0 until nb).toList.map { delta =>
        Day(Days.daysBetween(genesis, DateTime.now.minusDays(delta).withTimeAtStartOfDay).getDays)
      }
  }

  def make(userId: User.ID) = Activity(Id today userId)
}
