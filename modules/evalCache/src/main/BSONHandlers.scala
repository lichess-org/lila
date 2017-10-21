package lila.evalCache

import reactivemongo.bson._
import scalaz.NonEmptyList

import chess.format.Uci
import lila.db.dsl._
import lila.tree.Eval._

private object BSONHandlers {

  import EvalCacheEntry._

  private implicit val TrustBSONHandler = doubleAnyValHandler[Trust](_.value, Trust.apply)
  private implicit val KnodesBSONHandler = intAnyValHandler[Knodes](_.value, Knodes.apply)

  implicit val PvsHandler = new BSONHandler[BSONString, NonEmptyList[Pv]] {
    private def scoreWrite(s: Score): String = s.value.fold(_.value.toString, m => s"#${m.value}")
    private def scoreRead(str: String): Option[Score] =
      if (str startsWith "#") parseIntOption(str drop 1) map { m => Score mate Mate(m) }
      else parseIntOption(str) map { c => Score cp Cp(c) }
    private def movesWrite(moves: Moves): String = Uci writeListPiotr moves.value.toList
    private def movesRead(str: String): Option[Moves] =
      Uci readListPiotr str flatMap (_.toNel) map Moves.apply
    private val scoreSeparator = ':'
    private val pvSeparator = '/'
    private val pvSeparatorStr = pvSeparator.toString
    def read(bs: BSONString): NonEmptyList[Pv] = bs.value.split(pvSeparator).toList.map { pvStr =>
      pvStr.split(scoreSeparator) match {
        case Array(score, moves) => Pv(
          scoreRead(score) err s"Invalid score $score",
          movesRead(moves) err s"Invalid moves $moves"
        )
        case x => sys error s"Invalid PV $pvStr: ${x.toList} (in ${bs.value})"
      }
    }.toNel err s"Empty PVs ${bs.value}"
    def write(x: NonEmptyList[Pv]) = BSONString {
      x.toList.map { pv =>
        s"${scoreWrite(pv.score)}$scoreSeparator${movesWrite(pv.moves)}"
      } mkString pvSeparatorStr
    }
  }

  implicit val EntryIdHandler = new BSONHandler[BSONString, Id] {
    def read(bs: BSONString): Id = bs.value split ':' match {
      case Array(fen) => Id(chess.variant.Standard, SmallFen raw fen)
      case Array(variantId, fen) => Id(
        parseIntOption(variantId) flatMap chess.variant.Variant.apply err s"Invalid evalcache variant $variantId",
        SmallFen raw fen
      )
      case _ => sys error s"Invalid evalcache id ${bs.value}"
    }
    def write(x: Id) = BSONString {
      if (x.variant.standard || x.variant == chess.variant.FromPosition) x.smallFen.value
      else s"${x.variant.id}:${x.smallFen.value}"
    }
  }

  implicit val evalHandler = Macros.handler[Eval]
  implicit val entryHandler = Macros.handler[EvalCacheEntry]
}
