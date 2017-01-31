package lila.evalCache

import reactivemongo.bson._

import chess.format.{ FEN, Uci }
import lila.db.BSON
import lila.db.dsl._
import lila.tree.Eval._

object BSONHandlers {

  import EvalCacheEntry._

  private implicit val FenBSONHandler = stringAnyValHandler[FEN](_.value, FEN.apply)
  private implicit val TrustBSONHandler = doubleAnyValHandler[Trust](_.value, Trust.apply)
  private implicit val MultiPvBSONHandler = intAnyValHandler[MultiPv](
    _.value,
    v => MultiPv(v) err s"Invalid MultiPv = $v")

  implicit val IdHandler = new BSONHandler[BSONString, Id] {
    private val separator = '|'
    def read(bs: BSONString): Id = bs.value.split(separator) match {
      case Array(fen, multiPvStr) => Id(
        SmallFen(fen),
        parseIntOption(multiPvStr) flatMap MultiPv.apply err s"Invalid MultiPv $multiPvStr")
    }
    def write(id: Id) = BSONString(s"${id.fen.value}$separator${id.multiPv.value}")
  }

  implicit val UciHandler = new BSONHandler[BSONString, Uci] {
    def read(bs: BSONString): Uci = Uci(bs.value) err s"Bad UCI: ${bs.value}"
    def write(x: Uci) = BSONString(x.uci)
  }
  implicit val PvHandler = new BSONHandler[BSONString, Pv] {
    private val separator = " "
    def read(bs: BSONString): Pv = Pv(bs.value.split(separator).toList flatMap { Uci(_) })
    def write(x: Pv) = BSONString(x.value map (_.uci) mkString separator)
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

  implicit val evalHandler = Macros.handler[Eval]
  implicit val trustedEvalHandler = Macros.handler[TrustedEval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
