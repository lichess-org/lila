package lila.relay

import lila.db.dsl._
import lila.db.BSON
import reactivemongo.bson._

object BSONHandlers {

  implicit val relayIdHandler = stringAnyValHandler[Relay.Id](_.value, Relay.Id.apply)

  implicit val relayHandler = Macros.handler[Relay]
}
