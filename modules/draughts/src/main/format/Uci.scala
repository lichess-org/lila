package draughts
package format

import scala.collection.mutable.ListBuffer

sealed trait Uci {

  def uci: String
  def piotr: String

  def origDest: (Pos, Pos)

  def apply(situation: Situation): Valid[Move]

}

object Uci
  extends scalaz.std.OptionInstances
  with scalaz.syntax.ToTraverseOps {

  case class Move(
      orig: Pos,
      dest: Pos,
      promotion: Option[PromotableRole] = None,
      capture: Option[List[Pos]] = None
  ) extends Uci {

    def keys = orig.key + dest.key
    def uci = orig.key + capture.fold(dest.toString)(_.reverse.mkString)
    def shortUci = orig.key + capture.fold(dest.toString)(_.last.toString)

    def keysPiotr = orig.piotrStr + dest.piotrStr
    def piotr = keysPiotr + promotionString

    def promotionString = promotion.fold("")(_.forsyth.toString)

    def origDest = orig -> dest

    def apply(situation: Situation) = situation.move(orig, dest, promotion)

    def toSan = s"${orig.shortKey}${if (capture.nonEmpty) "x" else "-"}${dest.shortKey}"

  }

  object Move {

    def apply(move: String): Option[Move] = {
      if (move.length >= 6) {
        val capts = (for { c <- 2 until move.length by 2 } yield Pos.posAt(move.slice(c, c + 2))).toList.flatten
        for {
          orig <- Pos.posAt(move take 2)
          dest <- Pos.posAt(move.slice(move.length - 2, move.length))
        } yield Move(orig, dest, None, Some(capts.reverse))
      } else {
        for {
          orig ← Pos.posAt(move take 2)
          dest ← Pos.posAt(move drop 2 take 2)
          promotion = move lift 4 flatMap Role.promotable
        } yield Move(orig, dest, promotion)
      }
    }

    def piotr(move: String) = for {
      orig ← move.headOption flatMap Pos.piotr
      dest ← move lift 1 flatMap Pos.piotr
      promotion = move lift 2 flatMap Role.promotable
    } yield Move(orig, dest, promotion)

    def fromStrings(origS: String, destS: String, promS: Option[String]) = for {
      orig ← Pos.posAt(origS)
      dest ← Pos.posAt(destS)
      promotion = Role promotable promS
    } yield Move(orig, dest, promotion)

  }

  case class WithSan(uci: Uci, san: String)

  def apply(move: draughts.Move) = Uci.Move(move.orig, move.dest, move.promotion)

  def apply(move: String): Option[Uci] = Uci.Move(move)

  def piotr(move: String): Option[Uci] = Uci.Move.piotr(move)

  def readList(moves: String): Option[List[Uci]] =
    moves.split(' ').toList.map(apply).sequence

  def writeList(moves: List[Uci]): String =
    moves.map(_.uci) mkString " "

  def readListPiotr(moves: String): Option[List[Uci]] =
    moves.split(' ').toList.map(piotr).sequence

  def writeListPiotr(moves: List[Uci]): String =
    moves.map(_.piotr) mkString " "

}
