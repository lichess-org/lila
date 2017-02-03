package lila.evalCache

import reactivemongo.bson._
import scalaz.NonEmptyList

import chess.format.{ FEN, Uci }
import lila.db.BSON
import lila.db.dsl._
import lila.tree.Eval._

object BSONHandlers {

  import EvalCacheEntry._

  implicit val SmallFenBSONHandler = stringAnyValHandler[SmallFen](_.value, SmallFen.raw)
  private implicit val TrustBSONHandler = doubleAnyValHandler[Trust](_.value, Trust.apply)
  private implicit val KnodesBSONHandler = intAnyValHandler[Knodes](_.value, Knodes.apply)

  implicit val PvsHandler = new BSONHandler[BSONString, NonEmptyList[Pv]] {
    private def scoreWrite(s: Score): String = s.value.fold(_.value.toString, m => s"#${m.value}")
    private def scoreRead(str: String): Option[Score] =
      if (str startsWith "#") parseIntOption(str drop 1) map { m => Score mate Mate(m) }
      else parseIntOption(str) map { c => Score cp Cp(c) }
    private def movesWrite(moves: Moves): String = Uci writeListPiotr moves.value.list
    private def movesRead(str: String): Option[Moves] =
      Uci readListPiotr str flatMap (_.toNel) map Moves.apply
    private val scoreSeparator = ':'
    private val pvSeparator = '/'
    private val pvSeparatorStr = pvSeparator.toString
    def read(bs: BSONString): NonEmptyList[Pv] = bs.value.split(pvSeparator).toList.map { pvStr =>
      pvStr.split(scoreSeparator) match {
        case Array(score, moves) => Pv(
          scoreRead(score) err s"Invalid score $score",
          movesRead(moves) err s"Invalid moves $moves")
        case x => sys error s"Invalid PV $pvStr: ${x.toList} (in ${bs.value})"
      }
    }.toNel err s"Empty PVs ${bs.value}"
    def write(x: NonEmptyList[Pv]) = BSONString {
      x.list.map { pv =>
        s"${scoreWrite(pv.score)}$scoreSeparator${movesWrite(pv.moves)}"
      } mkString pvSeparatorStr
    }
  }

  implicit val evalHandler = Macros.handler[Eval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
