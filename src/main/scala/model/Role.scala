package lila
package model

import Pos._

sealed trait Role {
  val forsyth: Char
  def dirs: List[Direction]
  val trajectory = false
  val threatens = true
}
case object King extends Role {
  val forsyth = 'k'
  val dirs: List[Direction] = Queen.dirs
  override val threatens = false
}
case object Queen extends Role {
  val forsyth = 'q'
  val dirs: List[Direction] = Rook.dirs ::: Bishop.dirs
  override val trajectory = true
}
case object Rook extends Role {
  val forsyth = 'r'
  val dirs: List[Direction] = List(_.up, _.down, _.left, _.right)
  override val trajectory = true
}
case object Bishop extends Role {
  val forsyth = 'b'
  val dirs: List[Direction] = List(_.upLeft, _.upRight, _.downLeft, _.downRight)
  override val trajectory = true
}
case object Knight extends Role {
  val forsyth = 'n'
  val dirs: List[Direction] = List(
    _.up flatMap (_.upLeft),
    _.up flatMap (_.upRight),
    _.left flatMap (_.upLeft),
    _.left flatMap (_.downLeft),
    _.right flatMap (_.upRight),
    _.right flatMap (_.downRight),
    _.down flatMap (_.downLeft),
    _.down flatMap (_.downRight)
  )
}
case object Pawn extends Role {
  val forsyth = 'p'
  val dirs: List[Direction] = Nil
}

object Role {

  val all = List(King, Queen, Rook, Bishop, Knight, Pawn)

}
