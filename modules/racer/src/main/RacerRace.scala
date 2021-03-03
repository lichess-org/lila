package lila.racer

import org.joda.time.DateTime

import lila.user.User
import lila.socket.Socket

case class RacerRace(_id: RacerRace.Id, owner: RacerRace.Owner, createdAt: DateTime) {

  def id = _id
}

object RacerRace {

  case class Id(value: String) extends AnyVal with StringValue

  sealed trait Owner
  object Owner {
    case class User(userId: lila.user.User.ID) extends Owner
    case class Anon(sri: Socket.Sri)           extends Owner
  }

  def make(owner: Owner) = RacerRace(
    _id = Id(lila.common.ThreadLocalRandom nextString 8),
    owner = owner,
    createdAt = DateTime.now
  )
}
