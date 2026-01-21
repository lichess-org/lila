package lila.insight

import chess.{ Color, Role }
import chess.IntRating
import chess.eval.WinPercent
import chess.rating.IntRatingDiff
import reactivemongo.api.bson.*

import lila.analyse.AccuracyPercent
import lila.common.SimpleOpening
import lila.db.BSON
import lila.db.dsl.{ *, given }
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType
import lila.core.game.Source
import lila.game.BSONHandlers.sourceHandler

object BSONHandlers:

  given lila.db.NoDbHandler[WinPercent] with {}

  given BSONHandler[Role] = tryHandler(
    { case BSONString(v) => Role.allByForsyth.get(v.head).toTry(s"Invalid role $v") },
    e => BSONString(e.forsyth.toString)
  )
  given BSONHandler[RelativeStrength] = valueMapHandler(RelativeStrength.byId)(_.id)
  given BSONHandler[Result] = valueMapHandler(Result.byId)(_.id)
  given BSONHandler[Phase] = valueMapHandler(Phase.byId)(_.id)
  given BSONHandler[Termination] = valueMapHandler(Termination.byId)(_.id)
  given BSONHandler[MovetimeRange] = valueMapHandler(MovetimeRange.byId)(_.id)
  given BSONHandler[Castling] = valueMapHandler(Castling.byId)(_.id)
  given BSONHandler[MaterialRange] = valueMapHandler(MaterialRange.byId)(_.id)
  given BSONHandler[EvalRange] = valueMapHandler(EvalRange.byId)(_.id)
  given BSONHandler[WinPercentRange] = valueMapHandler(WinPercentRange.byPercent)(_.bottom.toInt)
  given BSONHandler[AccuracyPercentRange] = valueMapHandler(AccuracyPercentRange.byPercent)(_.bottom.toInt)
  given BSONHandler[ClockPercentRange] = valueMapHandler(ClockPercentRange.byPercent)(_.bottom.toInt)
  given BSONHandler[QueenTrade] = BSONBooleanHandler.as[QueenTrade](QueenTrade.apply, _.id)
  given BSONHandler[Blur] = BSONBooleanNullHandler.as[Blur](Blur.apply, _.id)
  given BSONHandler[CplRange] = valueMapHandler(CplRange.byId)(_.cpl)
  given BSONHandler[AccuracyPercent] = percentAsIntHandler[AccuracyPercent]
  given BSONHandler[WinPercent] = percentAsIntHandler[WinPercent]
  given BSONHandler[ClockPercent] = percentAsIntHandler[ClockPercent]

  private val BSONBooleanNullHandler = quickHandler[Boolean](
    { case BSONBoolean(v) => v; case BSONNull => false },
    v => if v then BSONBoolean(true) else BSONNull
  )

  given BSONHandler[TimeVariance] = BSONIntegerHandler.as[TimeVariance](
    i => TimeVariance(i.toFloat / TimeVariance.intFactor),
    v => (v.id * TimeVariance.intFactor).toInt
  )

  private[insight] given BSONDocumentHandler[lila.insight.DateRange] = Macros.handler
  private[insight] given BSONHandler[Period] = intAnyValHandler(_.days, Period.apply)

  given BSON[InsightMove] with
    def reads(r: BSON.Reader) =
      InsightMove(
        phase = r.get[Phase]("p"),
        tenths = r.intO("t"),
        clockPercent = r.getO[ClockPercent]("s"),
        role = r.get[Role]("r"),
        eval = r.intO("e"),
        cpl = r.intO("c"),
        winPercent = r.getO[WinPercent]("w"),
        accuracyPercent = r.getO[AccuracyPercent]("a"),
        material = r.int("i"),
        awareness = r.boolO("o"),
        luck = r.boolO("l"),
        blur = r.boolD("b"),
        timeCv = r.intO("v").map(v => v.toFloat / TimeVariance.intFactor)
      )
    def writes(w: BSON.Writer, b: InsightMove) =
      BSONDocument(
        "p" -> b.phase,
        "t" -> b.tenths,
        "s" -> b.clockPercent,
        "r" -> b.role,
        "e" -> b.eval,
        "c" -> b.cpl,
        "w" -> b.winPercent,
        "a" -> b.accuracyPercent,
        "i" -> b.material,
        "o" -> b.awareness,
        "l" -> b.luck,
        "b" -> w.boolO(b.blur),
        "v" -> b.timeCv.map(v => (v * TimeVariance.intFactor).toInt)
      )

  given BSON[InsightEntry] with
    import InsightEntry.BSONFields.*
    def reads(r: BSON.Reader) =
      InsightEntry(
        id = r.str(id),
        userId = r.get[UserId](userId),
        color = r.get[Color](color),
        perf = r.get[PerfType](perf),
        opening = r.getO[SimpleOpening](opening),
        myCastling = r.get[Castling](myCastling),
        rating = r.getO[IntRating](rating),
        opponentRating = r.getO[IntRating](opponentRating),
        opponentStrength = r.getO[RelativeStrength](opponentStrength),
        opponentCastling = r.get[Castling](opponentCastling),
        moves = r.get[List[InsightMove]](moves),
        queenTrade = r.get[QueenTrade](queenTrade),
        result = r.get[Result](result),
        termination = r.get[Termination](termination),
        ratingDiff = r.get[IntRatingDiff](ratingDiff),
        analysed = r.boolD(analysed),
        provisional = r.boolD(provisional),
        source = r.getO[Source](source),
        date = r.date(date)
      )
    def writes(w: BSON.Writer, e: InsightEntry) =
      BSONDocument(
        id -> e.id,
        userId -> e.userId,
        color -> e.color,
        perf -> PerfType(e.perf),
        opening -> e.opening,
        openingFamily -> e.opening.map(_.family),
        myCastling -> e.myCastling,
        rating -> e.rating,
        opponentRating -> e.opponentRating,
        opponentStrength -> e.opponentStrength,
        opponentCastling -> e.opponentCastling,
        moves -> e.moves,
        queenTrade -> e.queenTrade,
        result -> e.result,
        termination -> e.termination,
        ratingDiff -> e.ratingDiff,
        analysed -> w.boolO(e.analysed),
        provisional -> w.boolO(e.provisional),
        source -> e.source,
        date -> e.date
      )
