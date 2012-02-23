package lila
package model

import scala.annotation.tailrec

case class Piece(color: Color, role: Role) {

  type Vector = Pos ⇒ Option[Pos]

  def basicMoves(pos: Pos, board: Board): Set[Pos] = {

    role match {
      case Rook ⇒ {
        val occupation = board occupation !color
        val vectors: List[Vector] = List(_.up, _.down, _.left, _.right)

        //@tailrec
        def fwd(p: Pos, v: Vector): List[Pos] = v(p) map { next ⇒
          next :: fwd(next, v)
        } getOrElse Nil

        vectors flatMap { vector =>
          fwd(pos, vector)
        } toSet
      }
      case _ ⇒ Set.empty
    }
  }

  def is(c: Color) = c == color
  def is(r: Role) = r == role

  def forsyth: Char = if (color == White) role.forsyth.toUpper else role.forsyth

  override def toString = (color + " " + role).toLowerCase
}
