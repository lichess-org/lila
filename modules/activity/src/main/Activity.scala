package lila.activity

import org.joda.time.{ DateTime, Days }
import ornicar.scalalib.Zero

import lila.rating.PerfType
import lila.user.User

case class Activity(
    id: Activity.Id,
    games: Activity.Games,
    comps: Activity.CompAnalysis,
    posts: Activity.Posts,
    puzzles: Activity.Puzzles
) {

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
  case class RatingDiff(value: Int) extends AnyVal {
    def +(r: RatingDiff) = RatingDiff(value + r.value)
  }
  implicit val RatingDiffZero = Zero.instance(RatingDiff(0))
  implicit val ScoreZero = Zero.instance(Score(0, 0, 0, RatingDiffZero.zero))

  case class Posts(posts: Map[Posts.TopicId, List[Posts.PostId]]) {
    def total = posts.foldLeft(0)(_ + _._2.size)
    def +(postId: Posts.PostId, topicId: Posts.TopicId) = Posts {
      posts + (topicId -> (postId :: ~posts.get(topicId)))
    }
  }
  object Posts {
    case class TopicId(value: String) extends AnyVal
    case class PostId(value: String) extends AnyVal
  }
  implicit val PostsZero = Zero.instance(Posts(Map.empty))

  case class CompAnalysis(gameIds: List[GameId]) extends AnyVal {
    def +(gameId: GameId) = CompAnalysis(gameId :: gameIds)
  }
  case class GameId(value: String) extends AnyVal
  implicit val CompsZero = Zero.instance(CompAnalysis(Nil))

  case class Puzzles(win: PuzzleList, loss: PuzzleList) {
    def +(id: PuzzleId, isWin: Boolean) = if (isWin) copy(win = win + id) else copy(loss = loss + id)
  }
  case class PuzzleList(latest: List[PuzzleId], total: Int) {
    def +(id: PuzzleId) = PuzzleList(
      latest = (id :: latest) take 10,
      total = total + 1
    )
  }
  case class PuzzleId(value: Int) extends AnyVal
  implicit val PuzzleListZero = Zero.instance(PuzzleList(Nil, 0))
  implicit val PuzzlesZero = Zero.instance(Puzzles(PuzzleListZero.zero, PuzzleListZero.zero))

  def make(userId: User.ID) = Activity(
    id = Id today userId,
    games = GamesZero.zero,
    posts = PostsZero.zero,
    comps = CompsZero.zero,
    puzzles = PuzzlesZero.zero
  )
}
