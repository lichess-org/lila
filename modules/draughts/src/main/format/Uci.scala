package draughts
package format

import scala.collection.mutable.ListBuffer

sealed trait Uci {

  def uci: String
  def shortUci: String
  def piotr: String

  def origDest: (Pos, Pos)

  def apply(situation: Situation, finalSquare: Boolean = false): Valid[Move]

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

    def apply(situation: Situation, finalSquare: Boolean = false) = situation.move(orig, dest, promotion, finalSquare, none, capture)

    def toSan = s"${orig.shortKey}${if (capture.nonEmpty) "x" else "-"}${dest.shortKey}"

  }

  object Move {

    def apply(move: String, boardSize: Board.BoardSize): Option[Move] = {
      def posAt(f: String) = boardSize.pos.posAt(f)
      if (move.length >= 6) {
        val capts = (for { c <- 2 until move.length by 2 } yield posAt(move.slice(c, c + 2))).toList.flatten
        for {
          orig <- posAt(move take 2)
          dest <- posAt(move.slice(move.length - 2, move.length))
        } yield Move(orig, dest, None, Some(capts.reverse))
      } else {
        for {
          orig ← posAt(move take 2)
          dest ← posAt(move drop 2 take 2)
          promotion = move lift 4 flatMap Role.promotable
        } yield Move(orig, dest, promotion)
      }
    }

    def piotr(move: String, boardSize: Board.BoardSize) = for {
      orig ← move.headOption flatMap boardSize.pos.piotr
      dest ← move lift 1 flatMap boardSize.pos.piotr
      promotion = move lift 2 flatMap Role.promotable
    } yield Move(orig, dest, promotion)

    def fromStrings(origS: String, destS: String, promS: Option[String], boardSize: Board.BoardSize) = for {
      orig ← boardSize.pos.posAt(origS)
      dest ← boardSize.pos.posAt(destS)
      promotion = Role promotable promS
    } yield Move(orig, dest, promotion)

  }

  case class WithSan(uci: Uci, san: String)

  def apply(move: draughts.Move, withCaptures: Boolean) = Uci.Move(move.orig, move.dest, move.promotion, withCaptures ?? move.capture)

  def combine(uci1: Uci, uci2: Uci, boardSize: Board.BoardSize) = apply(uci1.uci + uci2.uci.drop(2), boardSize).getOrElse(Uci.Move(uci1.origDest._1, uci2.origDest._2))
  def combineSan(san1: String, san2: String) = san1.substring(0, san1.indexOf('x')) + san2.substring(san2.indexOf('x'))

  def apply(move: String, boardSize: Board.BoardSize): Option[Uci] = Uci.Move(move, boardSize)

  def piotr(move: String, boardSize: Board.BoardSize): Option[Uci] = Uci.Move.piotr(move, boardSize)

  def readList(moves: String, boardSize: Board.BoardSize): Option[List[Uci]] =
    moves.split(' ').toList.map(apply(_, boardSize)).sequence

  def writeList(moves: List[Uci]): String =
    moves.map(_.uci) mkString " "

  def readListPiotr(moves: String, boardSize: Board.BoardSize): Option[List[Uci]] =
    moves.split(' ').toList.map(piotr(_, boardSize)).sequence

  def writeListPiotr(moves: List[Uci]): String =
    moves.map(_.piotr) mkString " "

}
