package lila.activity

import org.joda.time.{ DateTime, Days }

import lila.user.User

import activities._

case class Activity(
    id: Activity.Id,
    games: Games,
    comps: CompAnalysis,
    posts: Posts,
    puzzles: Puzzles,
    learn: Learn,
    practice: Practice,
    simuls: Simuls,
    corres: Corres
) {

  def date = Activity.Day.genesis plusDays id.day.value
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
    def today = Day(Days.daysBetween(genesis, DateTime.now.withTimeAtStartOfDay).getDays)
    def recent(nb: Int): List[Day] = (0 to (nb - 1)).toList.map { delta =>
      Day(Days.daysBetween(genesis, DateTime.now.minusDays(delta).withTimeAtStartOfDay).getDays)
    }
  }

  def make(userId: User.ID) = Activity(
    id = Id today userId,
    games = GamesZero.zero,
    posts = PostsZero.zero,
    comps = CompsZero.zero,
    puzzles = PuzzlesZero.zero,
    learn = LearnZero.zero,
    practice = PracticeZero.zero,
    simuls = SimulsZero.zero,
    corres = CorresZero.zero
  )
}
