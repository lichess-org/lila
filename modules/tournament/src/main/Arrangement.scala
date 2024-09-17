package lila.tournament

import org.joda.time.DateTime

import shogi.Color

case class Arrangement(
    id: Arrangement.ID, // random
    order: Int,         // multiple arrangements between players can be set up
    tourId: Tournament.ID,
    user1: Arrangement.User,
    user2: Arrangement.User,
    name: Option[String] = none,
    color: Option[Color] = none, // user1 color
    gameId: Option[lila.game.Game.ID] = none,
    status: Option[shogi.Status] = none,
    winner: Option[lila.user.User.ID] = none,
    plies: Option[Int] = none,
    scheduledAt: Option[DateTime] = none,
    history: Arrangement.History = Arrangement.History.empty
) {

  def users = List(user1, user2)

  def userIds = users.map(_.id)

  def hasUser(userId: lila.user.User.ID) = userIds contains userId

  def user(userId: lila.user.User.ID) = users.find(_.id == userId)

  def opponentUser(userId: lila.user.User.ID) =
    if (userId == user1.id) user2.some
    else if (userId == user2.id) user1.some
    else none

  def updateUser(userId: lila.user.User.ID, f: (Arrangement.User) => Arrangement.User) =
    if (userId == user1.id) copy(user1 = f(user1))
    else if (userId == user2.id) copy(user2 = f(user2))
    else this

  def isWithinTolerance(date1: DateTime, date2: DateTime, toleranceSeconds: Int): Boolean =
    Math.abs(date1.getMillis - date2.getMillis) <= toleranceSeconds * 60000

  def setScheduledAt(userId: lila.user.User.ID, userScheduledAt: Option[DateTime]) = {
    val prevScheduledAt     = user(userId).flatMap(_.scheduledAt)
    val opponentScheduledAt = opponentUser(userId).flatMap(_.scheduledAt)
    val updated             = updateUser(userId, _.copy(scheduledAt = userScheduledAt))

    userScheduledAt.fold {
      updated.copy(
        scheduledAt = none,
        history =
          prevScheduledAt.fold(history)(psa => history.add(userId, psa.some, Arrangement.History.remove))
      )
    } { usa =>
      opponentScheduledAt
        .filter(isWithinTolerance(_, usa, 60))
        .fold {
          updated.copy(
            scheduledAt = none,
            history = history.add(userId, usa.some, Arrangement.History.propose)
          )
        } { osa =>
          updated.copy(
            scheduledAt = opponentScheduledAt,
            history = history.add(userId, osa.some, Arrangement.History.accept)
          )
        }
    }
  }

  def setReadyAt(userId: lila.user.User.ID, userReadyAt: Option[DateTime]) = {
    val prevReadyAt = user(userId).flatMap(_.readyAt)
    val updated     = updateUser(userId, _.copy(readyAt = userReadyAt))

    userReadyAt.fold {
      updated.copy(
        history =
          if (prevReadyAt.isDefined)
            history.add(userId, userReadyAt, Arrangement.History.notReady)
          else history
      )
    } { ura =>
      updated.copy(history =
        if (prevReadyAt.exists(isWithinTolerance(_, ura, 25)))
          history
        else history.add(userId, userReadyAt, Arrangement.History.ready)
      )
    }
  }

  def setSettings(settings: Arrangement.Settings) =
    copy(
      name = settings.name,
      color = settings.color,
      scheduledAt = settings.scheduledAt
    )

  def opponentIsReady(userId: lila.user.User.ID, maxSeconds: Int): Boolean = {
    val oppReady =
      ((user1.id == userId).option(user2) orElse (user2.id == userId).option(user1)).flatMap(_.readyAt)
    val cutoff = DateTime.now.minusSeconds(maxSeconds)
    oppReady.exists(_ isAfter cutoff)
  }

  def hasGame = gameId.isDefined

  def finished = status.exists(_ >= shogi.Status.Mate)
  def playing  = status.exists(_ < shogi.Status.Mate)

}

object Arrangement {

  type ID = String

  case class User(
      id: lila.user.User.ID,
      readyAt: Option[DateTime],
      scheduledAt: Option[DateTime]
  ) {
    def clear = copy(scheduledAt = none)
  }

  case class Settings(
      name: Option[String],
      color: Option[Color],
      // points: Points,
      // forceScheduledSpan: Option[Int], // minutes around scheduled when matching allowed
      scheduledAt: Option[DateTime]
  )

  case class History(list: List[History.Entry]) extends AnyVal {
    def add(userId: lila.user.User.ID, dateTime: Option[DateTime], action: History.Action) =
      History(
        (s"$userId${History.separator}${dateTime.fold("")(_.getSeconds.toString)}${History.separator}$action"
          .take(100) :: list).take(History.max)
      )

  }
  object History {
    val separator = ";"
    val max       = 10
    type Entry = String

    type Action = String
    val accept: Action   = "A"
    val remove: Action   = "M"
    val propose: Action  = "P"
    val ready: Action    = "R"
    val notReady: Action = "N"

    val empty = History(Nil)
  }

  // Arrangements are created when needed, so ID doesn't exist at the time.
  case class Lookup(
      tourId: Tournament.ID,
      users: (lila.user.User.ID, lila.user.User.ID),
      order: Option[Int]
  ) {
    def user1 = if (users._1 < users._2) users._1 else users._2
    def user2 = if (users._1 < users._2) users._2 else users._1
  }

  private[tournament] def make(
      lookup: Lookup
  ): Arrangement =
    Arrangement(
      id = lila.common.ThreadLocalRandom.nextString(8),
      order = ~lookup.order,
      tourId = lookup.tourId,
      user1 = Arrangement.User(
        id = lookup.user1,
        readyAt = none,
        scheduledAt = none
      ),
      user2 = Arrangement.User(
        id = lookup.user2,
        readyAt = none,
        scheduledAt = none
      )
    )
}
