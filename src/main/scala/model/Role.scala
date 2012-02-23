package lila
package model

import Pos._

sealed trait Role {
  val forsyth: Char
}
case object King extends Role {
  val forsyth = 'k'
}
case object Queen extends Role {
  val forsyth = 'q'
}
case object Rook extends Role {
  val forsyth = 'r'

  def vectors = List(List(^ _), List(> _), List(v _), List(< _))
}
case object Bishop extends Role {
  val forsyth = 'b'
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
