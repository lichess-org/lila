package lila.relay

import reactivemongo.api.bson._

import lila.db.dsl._

object BSONHandlers {

  implicit val relayIdHandler     = stringAnyValHandler[RelayRound.Id](_.value, RelayRound.Id.apply)
  implicit val relayTourIdHandler = stringAnyValHandler[RelayTour.Id](_.value, RelayTour.Id.apply)

  import RelayRound.Sync
  import Sync.{ Upstream, UpstreamIds, UpstreamUrl }
  implicit val upstreamUrlHandler = Macros.handler[UpstreamUrl]
  implicit val upstreamIdsHandler = Macros.handler[UpstreamIds]

  implicit val upstreamHandler = tryHandler[Upstream](
    {
      case d: BSONDocument if d.contains("url") => upstreamUrlHandler readTry d
      case d: BSONDocument if d.contains("ids") => upstreamIdsHandler readTry d
    },
    {
      case url: UpstreamUrl => upstreamUrlHandler.writeTry(url).get
      case ids: UpstreamIds => upstreamIdsHandler.writeTry(ids).get
    }
  )

  import SyncLog.Event
  implicit val syncLogEventHandler = Macros.handler[Event]

  implicit val syncLogHandler = isoHandler[SyncLog, Vector[Event]]((s: SyncLog) => s.events, SyncLog.apply _)

  implicit val syncHandler = Macros.handler[Sync]

  implicit val relayHandler = Macros.handler[RelayRound]

  implicit val relayTourHandler = Macros.handler[RelayTour]

  def readRelayWithTour(doc: Bdoc): Option[RelayRound.WithTour] =
    for {
      relay <- doc.asOpt[RelayRound]
      tour  <- doc.getAsOpt[RelayTour]("tour")
    } yield RelayRound.WithTour(relay, tour)
}
