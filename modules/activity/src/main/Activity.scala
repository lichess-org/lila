package lila.activity

import org.joda.time.{ DateTime, Days }
import ornicar.scalalib.Zero

import lila.rating.PerfType
import lila.user.User

case class Activity(
    _id: Activity.Id,
    games: Activity.Games,
    tours: Activity.Tours,
    comps: Activity.CompAnalysis,
    posts: Activity.Posts
) {

  def id = _id
  def userId = id.userId
  def day = id.day
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
  }

  case class Games(value: Map[PerfType, Score]) extends AnyVal {
    def add(pt: PerfType, score: Score) = copy(
      value = value + (pt -> value.get(pt).fold(score)(_ add score))
    )
  }
  implicit val GamesZero = Zero.instance(Games(Map.empty))

  case class Score(win: Int, loss: Int, draw: Int, rd: RatingDiff) {
    def add(s: Score) = copy(win = win + s.win, loss = loss + s.loss, draw = draw + s.draw, rd = rd + s.rd)
  }
  case class RatingDiff(by100: Int) extends AnyVal {
    def real = by100 / 100d
    def +(r: RatingDiff) = RatingDiff(by100 + r.by100)
  }
  implicit val ScoreZero = Zero.instance(Score(0, 0, 0, RatingDiff(0)))

  case class Tours(value: Map[Tours.TourId, Tours.Result]) extends AnyVal
  object Tours {
    case class TourId(value: String) extends AnyVal
    case class Result(rank: Int, points: Int, score: Score)
  }
  implicit val ToursZero = Zero.instance(Tours(Map.empty))

  case class Posts(posts: Map[Posts.ThreadId, List[Posts.PostId]]) {
    def total = posts.foldLeft(0)(_ + _._2.size)
  }
  object Posts {
    case class ThreadId(value: String) extends AnyVal
    case class PostId(value: String) extends AnyVal
  }
  implicit val PostsZero = Zero.instance(Posts(Map.empty))

  case class CompAnalysis(gameIds: List[GameId]) extends AnyVal
  case class GameId(value: String) extends AnyVal
  implicit val CompsZero = Zero.instance(CompAnalysis(Nil))

  def make(userId: User.ID) = Activity(
    _id = Id today userId,
    games = GamesZero.zero,
    tours = ToursZero.zero,
    posts = PostsZero.zero,
    comps = CompsZero.zero
  )
}
