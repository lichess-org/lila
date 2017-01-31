package lila.evalCache

import reactivemongo.bson._
import scalaz.NonEmptyList

import chess.format.{ FEN, Uci }
import lila.db.BSON
import lila.db.dsl._
import lila.tree.Eval._

object BSONHandlers {

  import EvalCacheEntry._

  implicit val SmallFenBSONHandler = stringAnyValHandler[SmallFen](
    _.value,
    v => SmallFen raw FEN(v))
  private implicit val TrustBSONHandler = doubleAnyValHandler[Trust](_.value, Trust.apply)

  implicit val UciHandler = new BSONHandler[BSONString, Uci] {
    def read(bs: BSONString): Uci = Uci(bs.value) err s"Bad UCI: ${bs.value}"
    def write(x: Uci) = BSONString(x.uci)
  }
  implicit val MovesHandler = new BSONHandler[BSONString, Moves] {
    private val separator = " "
    def read(bs: BSONString): Moves = Moves {
      bs.value.split(separator).toList.flatMap { Uci(_) }.toNel err s"Invalid Pv ${bs.value}"
    }
    def write(x: Moves) = BSONString(x.value.list map (_.uci) mkString separator)
  }
  implicit val ScoreHandler = new BSONHandler[BSONInteger, Score] {
    private val sillyThreshold = math.pow(10, 6).toInt
    def read(bs: BSONInteger): Score =
      if (bs.value > sillyThreshold) Score.mate(Mate(bs.value - sillyThreshold))
      else if (bs.value < -sillyThreshold) Score.mate(Mate(bs.value + sillyThreshold))
      else Score cp Cp(bs.value)
    def write(x: Score) = BSONInteger(x.value match {
      case Left(cp)                => cp.value
      case Right(mate) if mate > 0 => mate.value + sillyThreshold
      case Right(mate) if mate < 0 => mate.value - sillyThreshold
    })
  }

  implicit val pvHandler = Macros.handler[Pv]
  implicit val evalHandler = Macros.handler[Eval]
  implicit val trustedEvalHandler = Macros.handler[TrustedEval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
