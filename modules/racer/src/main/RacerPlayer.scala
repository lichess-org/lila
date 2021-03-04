package lila.racer

import org.joda.time.DateTime
import lila.user.User

case class RacerPlayer(id: RacerPlayer.Id, createdAt: DateTime, moves: Int) {

  import RacerPlayer.Id

  def userId: Option[User.ID] = id match {
    case Id.User(id) => id.some
    case _           => none
  }
}

object RacerPlayer {
  sealed trait Id
  object Id {
    case class User(userId: lila.user.User.ID) extends Id
    case class Anon(sessionId: String)         extends Id
    def apply(str: String) =
      if (str startsWith "@") Anon(str drop 1)
      else User(str)
  }

  def make(id: Id) = RacerPlayer(id = id, moves = 0, createdAt = DateTime.now)
}
