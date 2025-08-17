package lila.relay

import reactivemongo.api.bson.*

import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.core.fide.FideTC
import chess.tiebreak.Tiebreak
import chess.tiebreak.{ CutModifier, LimitModifier }

object BSONHandlers:

  given BSONHandler[RelayPlayersTextarea] = stringAnyValHandler(_.text, RelayPlayersTextarea(_))
  given BSONHandler[RelayTeamsTextarea] = stringAnyValHandler(_.text, RelayTeamsTextarea(_))
  given BSONHandler[RelayTour.Tier] = intAnyValHandler(_.v, RelayTour.Tier.byV(_))
  export lila.study.BSONHandlers.visibilityHandler

  import RelayRound.Sync
  import Sync.Upstream
  given upstreamUrlHandler: BSONDocumentHandler[Upstream.Url] = Macros.handler
  given upstreamUrlsHandler: BSONDocumentHandler[Upstream.Urls] = Macros.handler
  given upstreamIdsHandler: BSONDocumentHandler[Upstream.Ids] = Macros.handler
  given upstreamUsersHandler: BSONDocumentHandler[Upstream.Users] = Macros.handler

  given BSONHandler[Upstream] = new BSON[Upstream]:
    def reads(r: BSON.Reader): Upstream =
      if r.contains("url") then upstreamUrlHandler.readTry(r.doc).get
      else if r.contains("urls") then upstreamUrlsHandler.readTry(r.doc).get
      else if r.contains("ids") then upstreamIdsHandler.readTry(r.doc).get
      else if r.contains("users") then upstreamUsersHandler.readTry(r.doc).get
      else sys.error("Invalid Upstream BSON")
    def writes(w: BSON.Writer, up: Upstream) =
      val doc = up match
        case url: Upstream.Url => upstreamUrlHandler.writeTry(url).get
        case urls: Upstream.Urls => upstreamUrlsHandler.writeTry(urls).get
        case ids: Upstream.Ids => upstreamIdsHandler.writeTry(ids).get
        case users: Upstream.Users => upstreamUsersHandler.writeTry(users).get
      doc ++ up.roundIds.some.filter(_.nonEmpty).so(ids => $doc("roundIds" -> ids))

  import SyncLog.Event
  given BSONDocumentHandler[Event] = Macros.handler

  given BSONHandler[SyncLog] = isoHandler[SyncLog, Vector[Event]](_.events, SyncLog.apply)

  given BSONHandler[List[RelayGame.Slice]] =
    stringIsoHandler[List[RelayGame.Slice]](using RelayGame.Slices.iso)

  given BSONHandler[Sync.OnlyRound] = quickHandler[Sync.OnlyRound](
    {
      case BSONString(s) => Left(s)
      case BSONInteger(i) => Right(i)
    },
    _.fold(BSONString(_), BSONInteger(_))
  )

  given BSONDocumentHandler[Sync] = Macros.handler

  import RelayRound.Starts
  val startsAfterPrevious = "afterPrevious"
  given BSONHandler[Starts] = quickHandler[Starts](
    {
      case v: BSONDateTime => Starts.At(millisToInstant(v.value))
      case BSONString("afterPrevious") => Starts.AfterPrevious
    },
    {
      case Starts.At(time) => BSONDateTime(time.toMillis)
      case Starts.AfterPrevious => BSONString("afterPrevious")
    }
  )

  given BSONHandler[FideTC] = stringAnyValHandler[FideTC](_.toString, FideTC.valueOf)

  given BSONHandler[Tiebreak] = new BSON[Tiebreak]:

    def reads(r: BSON.Reader): Tiebreak = {
      for
        code <- r.getO[String]("code").flatMap(Tiebreak.Code.byStr.get): Option[Tiebreak.Code]
        tb <- Tiebreak(
          code,
          mkCutModifier = r
            .getO[String]("cutModifier")
            .flatMap(CutModifier.byCode.get)
            .orElse(CutModifier.None.some),
          mkLimitModifier = r.getO[Float]("limitModifier").flatMap(LimitModifier(_))
        )
      yield tb
    }.err(s"Invalid tiebreak ${r.debug}")

    def writes(w: BSON.Writer, t: Tiebreak) =
      $doc("code" -> t.code) ++
        t.foldModifier(
          $empty,
          cut => if cut == CutModifier.None then $empty else $doc("cutModifier" -> cut.code),
          limit => $doc("limitModifier" -> limit.value)
        )

  given BSONHandler[RelayRound.CustomScoring] = Macros.handler
  given BSONDocumentHandler[RelayRound] = Macros.handler

  given BSONDocumentHandler[RelayPinnedStream] = Macros.handler
  given BSONDocumentHandler[RelayTour.Spotlight] = Macros.handler
  given BSONDocumentHandler[RelayTour.Info] = Macros.handler
  given BSONDocumentHandler[RelayTour.Dates] = Macros.handler
  given BSONDocumentHandler[RelayTour] = Macros.handler

  given BSONDocumentHandler[RelayTour.TourPreview] = Macros.handler

  def readRoundWithTour(doc: Bdoc): Option[RelayRound.WithTour] = for
    round <- doc.asOpt[RelayRound]
    tour <- doc.getAsOpt[RelayTour]("tour")
  yield RelayRound.WithTour(round, tour)

  given BSONDocumentHandler[RelayGroup] = Macros.handler
