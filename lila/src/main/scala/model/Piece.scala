package lila
package model

import Pos._

sealed trait Role
case object King extends Role
case object Queen extends Role
case object Rook extends Role
case object Bishop extends Role
case object Knight extends Role
case object Pawn extends Role

sealed trait Color
case object White extends Color
case object Black extends Color

case class Piece(color: Color, role: Role) {

  override def toString = (color + " " + role).toLowerCase
}
