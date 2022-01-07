package shogi
package format.usi

import cats.data.Validated
import cats.implicits._

sealed trait Usi {

  def usi: String
  def uci: String
  def piotr: String

  def origDest: (Pos, Pos)

  def apply(situation: Situation): Validated[String, MoveOrDrop]
}

object Usi {

  case class Move(
      orig: Pos,
      dest: Pos,
      promotion: Boolean = false
  ) extends Usi {

    def usiKeys = orig.usiKey + dest.usiKey
    def usi     = usiKeys + promotionString

    def uciKeys = orig.uciKey + dest.uciKey
    def uci     = uciKeys + promotionString

    def keysPiotr = orig.piotrStr + dest.piotrStr
    def piotr     = keysPiotr + promotionString

    def promotionString = if (promotion) "+" else ""

    def origDest = orig -> dest

    def apply(situation: Situation) = {
      situation.move(orig, dest, promotion) map Left.apply
    }
  }

  object Move {

    def apply(move: String): Option[Move] =
      for {
        orig <- Pos.fromKey(move take 2)
        dest <- Pos.fromKey(move.slice(2, 4))
        promotion = if ((move lift 4) == Some('+')) true else false
      } yield Move(orig, dest, promotion)

    def piotr(move: String) =
      for {
        orig <- move.headOption flatMap Pos.piotr
        dest <- move lift 1 flatMap Pos.piotr
        promotion = if ((move lift 2) == Some('+')) true else false
      } yield Move(orig, dest, promotion)

  }

  case class Drop(role: Role, pos: Pos) extends Usi {

    def usi = s"${role.forsythUpper}*${pos.usiKey}"

    def uci = s"${role.forsythUpper}*${pos.uciKey}"

    def piotr = s"${role.forsythUpper}*${pos.piotrStr}"

    def origDest = pos -> pos

    def apply(situation: Situation) = situation.drop(role, pos) map Right.apply
  }

  object Drop {

    def apply(drop: String): Option[Drop] =
      for {
        role <- Role.allByForsythUpper.get(drop.takeWhile(_ != '*'))
        pos  <- Pos.fromKey(drop takeRight 2)
      } yield Drop(role, pos)

    def piotr(drop: String): Option[Drop] =
      for {
        role <- Role.allByForsyth.get(drop.takeWhile(_ != '*'))
        pos  <- drop.lastOption flatMap Pos.piotr
      } yield Drop(role, pos)

  }

  case class WithRole(usi: Usi, role: Role)

  def apply(move: shogi.Move) = Usi.Move(move.orig, move.dest, move.promotion)

  def apply(drop: shogi.Drop) = Usi.Drop(drop.piece.role, drop.pos)

  def apply(moveOrDrop: String): Option[Usi] =
    if (moveOrDrop contains '*')
      Usi.Drop(moveOrDrop)
    else Usi.Move(moveOrDrop)

  def piotr(moveOrDrop: String): Option[Usi] =
    if (moveOrDrop contains '*')
      Usi.Drop.piotr(moveOrDrop)
    else Usi.Move.piotr(moveOrDrop)

  def readList(moves: String): Option[List[Usi]] =
    readList(moves.split(' ').toList)

  def readList(moves: Seq[String]): Option[List[Usi]] =
    moves.toList.map(apply).sequence

  def writeList(moves: List[Usi]): String =
    moves.map(_.usi) mkString " "

  def readListPiotr(moves: String): Option[List[Usi]] =
    readListPiotr(moves.split(' ').toList)

  def readListPiotr(moves: Seq[String]): Option[List[Usi]] =
    moves.toList.map(piotr).sequence

  def writeListPiotr(moves: List[Usi]): String =
    moves.map(_.piotr) mkString " "
}
