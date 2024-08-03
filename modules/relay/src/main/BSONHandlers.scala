package lila.relay

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }

object BSONHandlers:

  given BSONHandler[RelayPlayersTextarea] = stringAnyValHandler(_.text, RelayPlayersTextarea(_))
  given BSONHandler[RelayTeamsTextarea]   = stringAnyValHandler(_.text, RelayTeamsTextarea(_))

  import RelayRound.Sync
  import Sync.Upstream
  given upstreamUrlHandler: BSONDocumentHandler[Upstream.Url]   = Macros.handler
  given upstreamUrlsHandler: BSONDocumentHandler[Upstream.Urls] = Macros.handler
  given upstreamIdsHandler: BSONDocumentHandler[Upstream.Ids]   = Macros.handler

  given BSONHandler[Upstream] = new BSON[Upstream]:
    def reads(r: BSON.Reader): Upstream =
      if r.contains("url") then upstreamUrlHandler.readTry(r.doc).get
      else if r.contains("urls") then upstreamUrlsHandler.readTry(r.doc).get
      else upstreamIdsHandler.readTry(r.doc).get
    def writes(w: BSON.Writer, up: Upstream) =
      val doc = up match
        case url: Upstream.Url   => upstreamUrlHandler.writeTry(url).get
        case urls: Upstream.Urls => upstreamUrlsHandler.writeTry(urls).get
        case ids: Upstream.Ids   => upstreamIdsHandler.writeTry(ids).get
      doc ++ up.roundIds.some.filter(_.nonEmpty).so(ids => $doc("roundIds" -> ids))

  import SyncLog.Event
  given BSONDocumentHandler[Event] = Macros.handler

  given BSONHandler[SyncLog] = isoHandler[SyncLog, Vector[Event]](_.events, SyncLog.apply)

  given BSONHandler[List[RelayGame.Slice]] =
    stringIsoHandler[List[RelayGame.Slice]](using RelayGame.Slices.iso)

  given BSONDocumentHandler[Sync] = Macros.handler

  import RelayRound.Starts
  val startsAfterPrevious = "afterPrevious"
  given BSONHandler[Starts] = quickHandler[Starts](
    {
      case v: BSONDateTime             => Starts.At(millisToInstant(v.value))
      case BSONString("afterPrevious") => Starts.AfterPrevious
    },
    {
      case Starts.At(time)      => BSONDateTime(time.toMillis)
      case Starts.AfterPrevious => BSONString("afterPrevious")
    }
  )

  given BSONDocumentHandler[RelayRound] = Macros.handler

  given BSONDocumentHandler[RelayPinnedStream]      = Macros.handler
  given BSONDocumentHandler[RelayTour.Spotlight]    = Macros.handler
  given BSONDocumentHandler[RelayTour.Info]         = Macros.handler
  given BSONDocumentHandler[RelayTour.Dates]        = Macros.handler
  given tourHandler: BSONDocumentHandler[RelayTour] = Macros.handler

  given BSONDocumentHandler[RelayTour.IdName] = Macros.handler

  def readRoundWithTour(doc: Bdoc): Option[RelayRound.WithTour] = for
    round <- doc.asOpt[RelayRound]
    tour  <- doc.getAsOpt[RelayTour]("tour")
  yield RelayRound.WithTour(round, tour)

  given BSONDocumentHandler[RelayGroup] = Macros.handler
