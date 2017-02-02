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
  private implicit val KnodesBSONHandler = intAnyValHandler[Knodes](_.value, Knodes.apply)

  implicit val PvHandler = new BSONHandler[BSONString, Pv] {
    private def scoreWrite(s: Score): String = s.value.fold(_.value.toString, m => s"#${m.value}")
    private def scoreRead(str: String): Option[Score] =
      if (str startsWith "#") parseIntOption(str drop 1) map { m => Score mate Mate(m) }
      else parseIntOption(str) map { c => Score cp Cp(c) }
    private def movesWrite(moves: Moves): String = Uci writeListPiotr moves.value.list
    private def movesRead(str: String): Option[Moves] =
      Uci readListPiotr str flatMap (_.toNel) map Moves.apply
    private val separator = '|'
    def read(bs: BSONString): Pv = bs.value.split(separator) match {
      case Array(score, moves) => Pv(
        scoreRead(score) err s"Invalid score $score",
        movesRead(moves) err s"Invalid moves $moves")
      case _ => sys error s"Invalid PV ${bs.value}"
    }
    def write(x: Pv) = BSONString(s"${scoreWrite(x.score)}$separator${movesWrite(x.moves)}")
  }

  implicit val evalHandler = Macros.handler[Eval]
  implicit val trustedEvalHandler = Macros.handler[TrustedEval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
