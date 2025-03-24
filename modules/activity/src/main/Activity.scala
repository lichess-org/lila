package lila.activity

import lila.common.LichessDay

import activities.*

case class Activity(
    id: Activity.Id,
    games: Option[Games] = None,
    forumPosts: Option[ForumPosts] = None,
    ublogPosts: Option[UblogPosts] = None,
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
):

  def date = id.day.toDate

  def interval = TimeInterval(date, date.plusDays(1))

  def isEmpty =
    !stream && List(
      games,
      forumPosts,
      ublogPosts,
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

object Activity:

  val recentNb = 7

  case class Id(userId: UserId, day: LichessDay)
  def today(userId: UserId) = Id(userId, LichessDay.today)

  case class WithUserId(activity: Activity, userId: UserId)

  def make(userId: UserId) = Activity(today(userId))
