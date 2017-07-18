package lila.activity

import org.joda.time.DateTime

import lila.user.User
import lila.rating.PerfType

case class Activity(
    _id: String, // random
    userId: User.ID,
    day: Activity.DaySinceSignup,
    games: Option[Activity.Games],
    tours: Option[Activity.Tours],
    comp: Option[Activity.CompAnalysis],
    posts: Option[Activity.Posts]
) {
}

object Activity {

  case class UserActivity(user: User, activity: Activity) {

    val createdDay = user.createdAt.withTimeAtStartOfDay
    val start = createdDay plusDays activity.day.value
    val end = start plusDays 1
  }

  // number of days since a user signed up
  case class DaySinceSignup(value: Int) extends AnyVal

  case class Games(value: Map[PerfType, Score]) extends AnyVal

  case class Score(win: Int, loss: Int, draw: Int)

  case class Tours(value: Map[Tours.TourId, Tours.Result]) extends AnyVal
  object Tours {
    case class TourId(value: String) extends AnyVal
    case class Result(rank: Int, points: Int, score: Score)
  }

  case class Posts(posts: Map[Posts.ThreadId, List[Posts.PostId]]) {
    def total = posts.foldLeft(0)(_ + _._2.size)
  }
  object Posts {
    case class ThreadId(value: String) extends AnyVal
    case class PostId(value: String) extends AnyVal
  }

  case class CompAnalysis(gameIds: List[GameId]) extends AnyVal
  case class GameId(value: String) extends AnyVal
}
