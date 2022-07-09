package lila.insight

import chess.{ Color, Role }
import reactivemongo.api.bson._

import lila.analyse.AnalyseBsonHandlers._
import lila.analyse.{ AccuracyPercent, WinPercent }
import lila.common.{ LilaOpening, LilaOpeningFamily }
import lila.db.BSON
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

object BSONHandlers {

  implicit val RoleBSONHandler = tryHandler[Role](
    { case BSONString(v) => Role.allByForsyth get v.head toTry s"Invalid role $v" },
    e => BSONString(e.forsyth.toString)
  )
  implicit val RelativeStrengthBSONHandler = valueMapHandler(RelativeStrength.byId)(_.id)
  implicit val ResultBSONHandler           = valueMapHandler(Result.byId)(_.id)
  implicit val PhaseBSONHandler            = valueMapHandler(Phase.byId)(_.id)
  implicit val TerminationBSONHandler      = valueMapHandler(Termination.byId)(_.id)
  implicit val MovetimeRangeBSONHandler    = valueMapHandler(MovetimeRange.byId)(_.id)
  implicit val CastlingBSONHandler         = valueMapHandler(Castling.byId)(_.id)
  implicit val MaterialRangeBSONHandler    = valueMapHandler(MaterialRange.byId)(_.id)
  implicit val EvalRangeBSONHandler        = valueMapHandler(EvalRange.byId)(_.id)
  implicit val WinPercentRangeReader       = valueMapHandler(WinPercentRange.byPercent)(_.bottom.toInt)
  implicit val AccuracyPercentRangeReader  = valueMapHandler(AccuracyPercentRange.byPercent)(_.bottom.toInt)
  implicit val ClockPercentRangeReader     = valueMapHandler(ClockPercentRange.byPercent)(_.bottom.toInt)
  implicit val QueenTradeBSONHandler       = BSONBooleanHandler.as[QueenTrade](QueenTrade.apply, _.id)
  implicit val CplRangeBSONHandler         = valueMapHandler(CplRange.byId)(_.cpl)

  private val BSONBooleanNullHandler = quickHandler[Boolean](
    { case BSONBoolean(v) => v; case BSONNull => false },
    v => if (v) BSONBoolean(true) else BSONNull
  )

  implicit val BlurBSONHandler = BSONBooleanNullHandler.as[Blur](Blur.apply, _.id)

  implicit val TimeVarianceBSONHandler = BSONIntegerHandler.as[TimeVariance](
    i => TimeVariance(i.toFloat / TimeVariance.intFactor),
    v => (v.id * TimeVariance.intFactor).toInt
  )

  implicit val DateRangeBSONHandler = Macros.handler[lila.insight.DateRange]
  implicit val PeriodBSONHandler    = intAnyValHandler[Period](_.days, Period.apply)
  implicit val clockPercentHandler: BSONHandler[ClockPercent] =
    percentAsIntHandler[ClockPercent](_.value, ClockPercent.apply)

  implicit def MoveBSONHandler = new BSON[InsightMove] {
    def reads(r: BSON.Reader) =
      InsightMove(
        phase = r.get[Phase]("p"),
        tenths = r.get[Int]("t"),
        clockPercent = r.get[ClockPercent]("s"),
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
  }

  implicit def EntryBSONHandler = new BSON[InsightEntry] {
    import InsightEntry.BSONFields._
    def reads(r: BSON.Reader) =
      InsightEntry(
        id = r.str(id),
        number = r.int(number),
        userId = r.str(userId),
        color = r.get[Color](color),
        perf = r.get[PerfType](perf),
        opening = r.getO[LilaOpening](opening),
        myCastling = r.get[Castling](myCastling),
        rating = r.intO(rating),
        opponentRating = r.intO(opponentRating),
        opponentStrength = r.getO[RelativeStrength](opponentStrength),
        opponentCastling = r.get[Castling](opponentCastling),
        moves = r.get[List[InsightMove]](moves),
        queenTrade = r.get[QueenTrade](queenTrade),
        result = r.get[Result](result),
        termination = r.get[Termination](termination),
        ratingDiff = r.int(ratingDiff),
        analysed = r.boolD(analysed),
        provisional = r.boolD(provisional),
        date = r.date(date)
      )
    def writes(w: BSON.Writer, e: InsightEntry) =
      BSONDocument(
        id               -> e.id,
        number           -> e.number,
        userId           -> e.userId,
        color            -> e.color,
        perf             -> e.perf,
        opening          -> e.opening,
        openingFamily    -> e.opening.map(_.family),
        myCastling       -> e.myCastling,
        rating           -> e.rating,
        opponentRating   -> e.opponentRating,
        opponentStrength -> e.opponentStrength,
        opponentCastling -> e.opponentCastling,
        moves            -> e.moves,
        queenTrade       -> e.queenTrade,
        result           -> e.result,
        termination      -> e.termination,
        ratingDiff       -> e.ratingDiff,
        analysed         -> w.boolO(e.analysed),
        provisional      -> w.boolO(e.provisional),
        date             -> e.date
      )
  }
}
