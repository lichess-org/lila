package lila
package model

import Pos._

sealed trait Role {
  val forsyth: Char
}
sealed trait Directed {
  def dirs: List[Direction]
}
case object King extends Role {
  val forsyth = 'k'
}
case object Queen extends Role with Directed {
  val forsyth = 'q'
  val dirs: List[Direction] = Rook.dirs ::: Bishop.dirs
}
case object Rook extends Role with Directed {
  val forsyth = 'r'
  val dirs: List[Direction] = List(_.up, _.down, _.left, _.right)
}
case object Bishop extends Role with Directed {
  val forsyth = 'b'
  val dirs: List[Direction] = List(_.upLeft, _.upRight, _.downLeft, _.downRight)
}
case object Knight extends Role {
  val forsyth = 'n'
}
case object Pawn extends Role {
  val forsyth = 'p'
}

object Role {

  val all = List(King, Queen, Rook, Bishop, Knight, Pawn)

}
