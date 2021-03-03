package lila.racer

import reactivemongo.api.bson._

import lila.db.dsl._
import lila.socket.Socket.Sri

private object RacerBsonHandlers {

  import RacerRace.Owner

  implicit val raceIdHandler = stringAnyValHandler[RacerRace.Id](_.value, RacerRace.Id.apply)

  implicit val ownerHandler = BSONStringHandler.as[Owner](
    str =>
      if (str startsWith "@") Owner.Anon(Sri(str drop 1))
      else Owner.User(str),
    {
      case Owner.Anon(Sri(value)) => s"@$value"
      case Owner.User(id)         => id
    }
  )

  implicit val raceBSONHandler = Macros.handler[RacerRace]
}
