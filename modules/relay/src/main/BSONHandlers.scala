package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  given BSONHandler[RelayPlayersTextarea] = stringAnyValHandler(_.text, RelayPlayersTextarea(_))
  given BSONHandler[RelayTeamsTextarea]   = stringAnyValHandler(_.text, RelayTeamsTextarea(_))

  import RelayRound.Sync
  import Sync.Upstream
  given upstreamUrlHandler: BSONDocumentHandler[Upstream.Url]   = Macros.handler
  given upstreamUrlsHandler: BSONDocumentHandler[Upstream.Urls] = Macros.handler
  given upstreamIdsHandler: BSONDocumentHandler[Upstream.Ids]   = Macros.handler

  given BSONHandler[Upstream] = tryHandler(
    {
      case d: BSONDocument if d.contains("url")  => upstreamUrlHandler.readTry(d)
      case d: BSONDocument if d.contains("urls") => upstreamUrlsHandler.readTry(d)
      case d: BSONDocument if d.contains("ids")  => upstreamIdsHandler.readTry(d)
    },
    {
      case url: Upstream.Url   => upstreamUrlHandler.writeTry(url).get
      case urls: Upstream.Urls => upstreamUrlsHandler.writeTry(urls).get
      case ids: Upstream.Ids   => upstreamIdsHandler.writeTry(ids).get
    }
  )

  import SyncLog.Event
  given BSONDocumentHandler[Event] = Macros.handler

  given BSONHandler[SyncLog] = isoHandler[SyncLog, Vector[Event]](_.events, SyncLog.apply)

  given BSONHandler[List[RelayGame.Slice]] =
    stringIsoHandler[List[RelayGame.Slice]](using RelayGame.Slices.iso)

  given BSONDocumentHandler[Sync] = Macros.handler

  given BSONDocumentHandler[RelayRound] = Macros.handler

  // private given BSONHandler[play.api.i18n.Lang]     = langByCodeHandler
  given BSONDocumentHandler[RelayTour.Spotlight]    = Macros.handler
  given tourHandler: BSONDocumentHandler[RelayTour] = Macros.handler

  given BSONDocumentHandler[RelayTour.IdName] = Macros.handler

  def readRoundWithTour(doc: Bdoc): Option[RelayRound.WithTour] = for
    round <- doc.asOpt[RelayRound]
    tour  <- doc.getAsOpt[RelayTour]("tour")
  yield RelayRound.WithTour(round, tour)

  given BSONDocumentHandler[RelayGroup] = Macros.handler
