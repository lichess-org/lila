package lila.relay

import lila.db.dsl._
import lila.db.BSON
import reactivemongo.bson._

object BSONHandlers {

  implicit val relayIdHandler = stringAnyValHandler[Relay.Id](_.value, Relay.Id.apply)

  import SyncLog.Event
  implicit val syncLogEventHandler = Macros.handler[Event]

  implicit val syncLogHandler = isoHandler[SyncLog, List[Event], Barr]((s: SyncLog) => s.events, SyncLog.apply _)

  implicit val relayHandler = Macros.handler[Relay]
}
