package lila.activity

import activities._
import org.joda.time.Interval

import lila.common.Day
import lila.user.User

case class Activity(
    id: Activity.Id,
    games: Option[Games] = None,
    posts: Option[Posts] = None,
    puzzles: Option[Puzzles] = None,
    storm: Option[Storm] = None,
    racer: Option[Racer] = None,
    streak: Option[Streak] = None,
    learn: Option[Learn] = None,
    practice: Option[Practice] = None,
    simuls: Option[Simuls] = None,
    corres: Option[Corres] = None,
    patron: Option[Patron] = None,
    follows: Option[Follows] = None,
    studies: Option[Studies] = None,
    teams: Option[Teams] = None,
    swisses: Option[Swisses] = None,
    stream: Boolean = false
) {

  def date = id.day.toDate

  def interval = new Interval(date, date plusDays 1)

  def isEmpty =
    !stream && List(
      games,
      posts,
      puzzles,
      storm,
      racer,
      streak,
      learn,
      practice,
      simuls,
      corres,
      patron,
      follows,
      studies,
      teams,
      swisses
    )
      .forall(_.isEmpty)
}

object Activity {

  case class Id(userId: User.ID, day: Day)
  object Id {
    def today(userId: User.ID) = Id(userId, Day.today)
  }

  case class WithUserId(activity: Activity, userId: User.ID)

  def make(userId: User.ID) = Activity(Id today userId)
}
