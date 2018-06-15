package lila.relay

import lila.db.dsl._
import reactivemongo.bson._

object BSONHandlers {

  import lila.study.BSONHandlers.LikesBSONHandler

  implicit val relayIdHandler = stringAnyValHandler[Relay.Id](_.value, Relay.Id.apply)

  import Relay.Sync
  import Sync.Upstream
  private implicit val SourceUpstreamHandler = new lila.db.BSON[Upstream] {
    def reads(r: lila.db.BSON.Reader) = r.str("k") match {
      case "dgt-one" => Upstream.DgtOneFile(r str "url")
      case "dgt-many" => Upstream.DgtManyFiles(r str "url")
      case k => sys error s"Invalid relay source upstream $k"
    }
    def writes(w: lila.db.BSON.Writer, u: Upstream) = $doc("k" -> u.key, "url" -> u.url)
  }

  import SyncLog.Event
  implicit val syncLogEventHandler = Macros.handler[Event]

  implicit val syncLogHandler = isoHandler[SyncLog, Vector[Event], Barr]((s: SyncLog) => s.events, SyncLog.apply _)

  implicit val syncHandler = Macros.handler[Sync]

  implicit val relayHandler = Macros.handler[Relay]
}
