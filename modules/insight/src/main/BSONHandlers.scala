package lila.insight

import reactivemongo.api.bson._

import chess.opening.{ Ecopening, EcopeningDB }
import chess.{ Color, Role }
import lila.db.BSON
import lila.db.dsl._
import lila.rating.BSONHandlers.perfTypeIdHandler
import lila.rating.PerfType

private object BSONHandlers {

  implicit val EcopeningBSONHandler = tryHandler[Ecopening](
    { case BSONString(v) => EcopeningDB.allByEco get v toTry s"Invalid ECO $v" },
    e => BSONString(e.eco)
  )
  implicit val RelativeStrengthBSONHandler = tryHandler[RelativeStrength](
    { case BSONInteger(v) => RelativeStrength.byId get v toTry s"Invalid relative strength $v" },
    e => BSONInteger(e.id)
  )
  implicit val ResultBSONHandler = tryHandler[Result](
    { case BSONInteger(v) => Result.byId get v toTry s"Invalid result $v" },
    e => BSONInteger(e.id)
  )

  implicit val PhaseBSONHandler = tryHandler[Phase](
    { case BSONInteger(v) => Phase.byId get v toTry s"Invalid phase $v" },
    e => BSONInteger(e.id)
  )
  implicit val RoleBSONHandler = tryHandler[Role](
    { case BSONString(v) => Role.allByForsyth get v.head toTry s"Invalid role $v" },
    e => BSONString(e.forsyth.toString)
  )
  implicit val TerminationBSONHandler = tryHandler[Termination](
    { case BSONInteger(v) => Termination.byId get v toTry s"Invalid termination $v" },
    e => BSONInteger(e.id)
  )
  implicit val MovetimeRangeBSONHandler = tryHandler[MovetimeRange](
    { case BSONInteger(v) => MovetimeRange.byId get v toTry s"Invalid movetime range $v" },
    e => BSONInteger(e.id)
  )
  implicit val CastlingBSONHandler = tryHandler[Castling](
    { case BSONInteger(v) => Castling.byId get v toTry s"Invalid Castling $v" },
    e => BSONInteger(e.id)
  )
  implicit val MaterialRangeBSONHandler = tryHandler[MaterialRange](
    { case BSONInteger(v) => MaterialRange.byId get v toTry s"Invalid material range $v" },
    e => BSONInteger(e.id)
  )
  implicit val EvalRangeBSONHandler = tryHandler[EvalRange](
    { case BSONInteger(v) => EvalRange.byId get v toTry s"Invalid eval range $v" },
    e => BSONInteger(e.id)
  )
  implicit val QueenTradeBSONHandler = BSONBooleanHandler.as[QueenTrade](QueenTrade.apply, _.id)

  private val BSONBooleanNullHandler = quickHandler[Boolean](
    { case BSONBoolean(v) => v; case BSONNull => false },
    v => if (v) BSONBoolean(true) else BSONNull
  )

  implicit val BlurBSONHandler = BSONBooleanNullHandler.as[Blur](Blur.apply, _.id)

  implicit val TimeVarianceBSONHandler = BSONIntegerHandler.as[TimeVariance](
    i => TimeVariance(i.toFloat / TimeVariance.intFactor),
    v => (v.id * TimeVariance.intFactor).toInt
  )

  implicit val CplRangeBSONHandler = tryHandler[CplRange](
    { case BSONInteger(v) => CplRange.byId get v toTry s"Invalid CPL range $v" },
    e => BSONInteger(e.cpl)
  )

  implicit val DateRangeBSONHandler = Macros.handler[lila.insight.DateRange]

  implicit val PeriodBSONHandler = intIsoHandler(lila.common.Iso.int[Period](Period.apply, _.days))

  implicit def MoveBSONHandler =
    new BSON[InsightMove] {
      def reads(r: BSON.Reader) =
        InsightMove(
          phase = r.get[Phase]("p"),
          tenths = r.get[Int]("t"),
          role = r.get[Role]("r"),
          eval = r.intO("e"),
          mate = r.intO("m"),
          cpl = r.intO("c"),
          material = r.int("i"),
          opportunism = r.boolO("o"),
          luck = r.boolO("l"),
          blur = r.boolD("b"),
          timeCv = r.intO("v").map(v => v.toFloat / TimeVariance.intFactor)
        )
      def writes(w: BSON.Writer, b: InsightMove) =
        BSONDocument(
          "p" -> b.phase,
          "t" -> b.tenths,
          "r" -> b.role,
          "e" -> b.eval,
          "m" -> b.mate,
          "c" -> b.cpl,
          "i" -> b.material,
          "o" -> b.opportunism,
          "l" -> b.luck,
          "b" -> w.boolO(b.blur),
          "v" -> b.timeCv.map(v => (v * TimeVariance.intFactor).toInt)
        )
    }

  implicit def EntryBSONHandler =
    new BSON[InsightEntry] {
      import InsightEntry.BSONFields._
      def reads(r: BSON.Reader) =
        InsightEntry(
          id = r.str(id),
          number = r.int(number),
          userId = r.str(userId),
          color = r.get[Color](color),
          perf = r.get[PerfType](perf),
          eco = r.getO[Ecopening](eco),
          myCastling = r.get[Castling](myCastling),
          opponentRating = r.int(opponentRating),
          opponentStrength = r.get[RelativeStrength](opponentStrength),
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
          eco              -> e.eco,
          myCastling       -> e.myCastling,
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
