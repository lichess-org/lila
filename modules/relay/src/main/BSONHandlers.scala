package lila.relay

import reactivemongo.api.bson.*

import lila.db.dsl.{ *, given }

object BSONHandlers:

  given BSONHandler[RelayPlayersTextarea] = stringAnyValHandler(_.text, RelayPlayersTextarea(_))
  given BSONHandler[RelayTeamsTextarea]   = stringAnyValHandler(_.text, RelayTeamsTextarea(_))

  import RelayRound.Sync
  import Sync.{ Upstream, UpstreamIds, UpstreamUrl, UpstreamLcc, UpstreamUrls, FetchableUpstream }
  given upstreamUrlHandler: BSONDocumentHandler[UpstreamUrl] = Macros.handler
  given upstreamLccHandler: BSONDocumentHandler[UpstreamLcc] = Macros.handler
  given BSONHandler[FetchableUpstream] = tryHandler(
    {
      case d: BSONDocument if d.contains("url") => upstreamUrlHandler.readTry(d)
      case d: BSONDocument if d.contains("lcc") => upstreamLccHandler.readTry(d)
    },
    {
      case url: UpstreamUrl => upstreamUrlHandler.writeTry(url).get
      case lcc: UpstreamLcc => upstreamLccHandler.writeTry(lcc).get
    }
  )
  given upstreamUrlsHandler: BSONDocumentHandler[UpstreamUrls] = Macros.handler
  given upstreamIdsHandler: BSONDocumentHandler[UpstreamIds]   = Macros.handler

  given BSONHandler[Upstream] = tryHandler(
    {
      case d: BSONDocument if d.contains("url")  => upstreamUrlHandler.readTry(d)
      case d: BSONDocument if d.contains("lcc")  => upstreamLccHandler.readTry(d)
      case d: BSONDocument if d.contains("urls") => upstreamUrlsHandler.readTry(d)
      case d: BSONDocument if d.contains("ids")  => upstreamIdsHandler.readTry(d)
    },
    {
      case url: UpstreamUrl   => upstreamUrlHandler.writeTry(url).get
      case lcc: UpstreamLcc   => upstreamLccHandler.writeTry(lcc).get
      case urls: UpstreamUrls => upstreamUrlsHandler.writeTry(urls).get
      case ids: UpstreamIds   => upstreamIdsHandler.writeTry(ids).get
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
