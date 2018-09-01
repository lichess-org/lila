package lidraughts.relay

import lidraughts.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  import lidraughts.study.BSONHandlers.LikesBSONHandler

  implicit val relayIdHandler = stringAnyValHandler[Relay.Id](_.value, Relay.Id.apply)

  import Relay.Sync
  import Sync.Upstream
  implicit val upstreamHandler = Macros.handler[Upstream]

  import SyncLog.Event
  implicit val syncLogEventHandler = Macros.handler[Event]

  implicit val syncLogHandler = isoHandler[SyncLog, Vector[Event], Barr]((s: SyncLog) => s.events, SyncLog.apply _)

  implicit val syncHandler = Macros.handler[Sync]

  implicit val relayHandler = Macros.handler[Relay]
}
