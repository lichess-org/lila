package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  given BSONHandler[RelayPlayers] = stringAnyValHandler(_.text, RelayPlayers(_))
  given BSONHandler[RelayTeams]   = stringAnyValHandler(_.text, RelayTeams(_))

  import RelayRound.Sync
  import Sync.{ Upstream, UpstreamIds, UpstreamUrl }
  given upstreamUrlHandler: BSONDocumentHandler[UpstreamUrl] = Macros.handler
  given upstreamIdsHandler: BSONDocumentHandler[UpstreamIds] = Macros.handler

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

  given BSONHandler[SyncLog] = isoHandler[SyncLog, Vector[Event]](_.events, SyncLog.apply)

  given BSONDocumentHandler[Sync] = Macros.handler

  given BSONDocumentHandler[RelayRound] = Macros.handler

  private given BSONHandler[play.api.i18n.Lang]     = langByCodeHandler
  given BSONDocumentHandler[RelayTour.Spotlight]    = Macros.handler
  given tourHandler: BSONDocumentHandler[RelayTour] = Macros.handler

  def readRoundWithTour(doc: Bdoc): Option[RelayRound.WithTour] = for
    round <- doc.asOpt[RelayRound]
    tour  <- doc.getAsOpt[RelayTour]("tour")
  yield RelayRound.WithTour(round, tour)
