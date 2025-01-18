package lila.tournament

import org.joda.time.DateTime
import shogi.Color

case class Arrangement(
    id: Arrangement.ID, // random
    tourId: Tournament.ID,
    user1: Arrangement.User,
    user2: Arrangement.User,
    name: Option[String] = none,
    color: Option[Color] = none, // user1 color
    points: Option[Arrangement.Points] = none,
    gameId: Option[lila.game.Game.ID] = none,
    startedAt: Option[DateTime] = none,
    status: Option[shogi.Status] = none,
    winner: Option[lila.user.User.ID] = none,
    plies: Option[Int] = none,
    scheduledAt: Option[DateTime] = none,
    allowGameBefore: Option[Int] = none, // minutes
    lockedScheduledAt: Boolean = false,
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

  def canGameStart =
    (scheduledAt, allowGameBefore) match {
      case (Some(scheduled), Some(minutes)) =>
        DateTime.now.plusMinutes(minutes).isAfter(scheduled)
      case _ => true
    }

  def setScheduledAt(userId: lila.user.User.ID, userScheduledAt: Option[DateTime]) = {
    val prevScheduledAt     = user(userId).flatMap(_.scheduledAt)
    val opponentScheduledAt = opponentUser(userId).flatMap(_.scheduledAt)
    val updated             = updateUser(userId, _.copy(scheduledAt = userScheduledAt))

    userScheduledAt.fold {
      updated.copy(
        scheduledAt = none,
        history =
          prevScheduledAt.fold(history)(psa => history.add(userId.some, psa.some, Arrangement.History.remove))
      )
    } { usa =>
      opponentScheduledAt
        .filter(isWithinTolerance(_, usa, 60))
        .fold {
          updated.copy(
            scheduledAt = none,
            history = history.add(userId.some, usa.some, Arrangement.History.propose)
          )
        } { osa =>
          updated.copy(
            scheduledAt = opponentScheduledAt,
            history = history.add(userId.some, osa.some, Arrangement.History.accept)
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
            history.add(userId.some, userReadyAt, Arrangement.History.notReady)
          else history
      )
    } { ura =>
      updated.copy(history =
        if (prevReadyAt.exists(isWithinTolerance(_, ura, 25)))
          history
        else history.add(userId.some, userReadyAt, Arrangement.History.ready)
      )
    }
  }

  def startGame(gid: lila.game.Game.ID, color: Color) = {
    val now = DateTime.now
    copy(
      user1 = user1.clearall,
      user2 = user2.clearall,
      gameId = gid.some,
      startedAt = now.some,
      color = color.some,
      history = history.add(none, now.some, Arrangement.History.starts)
    )
  }

  def setSettings(settings: Arrangement.Settings) =
    copy(
      name = settings.name,
      color = settings.color,
      points = settings.points,
      scheduledAt = settings.scheduledAt,
      allowGameBefore = settings.allowGameBefore,
      lockedScheduledAt = settings.scheduledAt.isDefined
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
    def clearall = copy(scheduledAt = none, readyAt = none)
  }

  case class Settings(
      name: Option[String],
      color: Option[Color],
      points: Option[Points],
      scheduledAt: Option[DateTime],
      allowGameBefore: Option[Int]
  )

  case class Points(loss: Int, draw: Int, win: Int)
  object Points {
    val default = Points(1, 2, 3)
    val max     = 100
    def apply(s: String): Option[Points] =
      s.split(";").toList match {
        case l :: d :: w :: Nil => {
          def parseNum(digit: String) = digit.toIntOption.map(_ atLeast 0 atMost max)
          for {
            loss <- parseNum(l)
            draw <- parseNum(d)
            win  <- parseNum(w)
          } yield Points(loss, draw, win)
        }
        case _ => None
      }
  }

  case class History(list: List[History.Entry]) extends AnyVal {
    def add(userId: Option[lila.user.User.ID], dateTime: Option[DateTime], action: History.Action) =
      History(
        (s"${~userId}${History.separator}${dateTime.fold("")(_.getSeconds.toString)}${History.separator}$action"
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
    val starts: Action   = "S"

    val empty = History(Nil)
  }

  case class Lookup(
      id: Option[ID], // Arrangements are created when needed, so ID might not exist at the time
      tourId: Tournament.ID,
      users: (lila.user.User.ID, lila.user.User.ID)
  ) {
    def userList = List(users._1, users._2)
  }

  private[tournament] def make(
      tourId: Tournament.ID,
      users: (lila.user.User.ID, lila.user.User.ID)
  ): Arrangement =
    Arrangement(
      id = lila.common.ThreadLocalRandom.nextString(8),
      tourId = tourId,
      user1 = Arrangement.User(
        id = users._1,
        readyAt = none,
        scheduledAt = none
      ),
      user2 = Arrangement.User(
        id = users._2,
        readyAt = none,
        scheduledAt = none
      )
    )
}
