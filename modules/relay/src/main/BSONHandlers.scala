package lila.relay

import reactivemongo.api.bson._

import lila.db.dsl.{ *, given }

object BSONHandlers {

  implicit val relayIdHandler      = stringAnyValHandler[RelayRound.Id](_.value, RelayRound.Id.apply)
  implicit val relayTourIdHandler  = stringAnyValHandler[RelayTour.Id](_.value, RelayTour.Id.apply)
  given BSONHandler[RelayPlayers] = stringAnyValHandler(_.text, RelayPlayers.apply)

  import RelayRound.Sync
  import Sync.{ Upstream, UpstreamIds, UpstreamUrl }
  given BSONDocumentHandler[UpstreamUrl] = Macros.handler
  given BSONDocumentHandler[UpstreamIds] = Macros.handler

  given BSONHandler[Upstream] = tryHandler(
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
  given BSONDocumentHandler[Event] = Macros.handler

  implicit val syncLogHandler = isoHandler[SyncLog, Vector[Event]]((s: SyncLog) => s.events, SyncLog.apply _)

  given BSONDocumentHandler[Sync] = Macros.handler

  given BSONDocumentHandler[RelayRound] = Macros.handler

  given BSONDocumentHandler[RelayTour] = Macros.handler

  def readRoundWithTour(doc: Bdoc): Option[RelayRound.WithTour] = for {
    round <- doc.asOpt[RelayRound]
    tour  <- doc.getAsOpt[RelayTour]("tour")
  } yield RelayRound.WithTour(round, tour)
}
