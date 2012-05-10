package lila.chess

import Pos._

sealed trait Role {
  val forsyth: Char
  lazy val pgn: Char = forsyth.toUpper
  val attacker: Boolean = true
  val projection: Boolean = false
  def dirs: List[Direction]
}
sealed trait PromotableRole extends Role

case object King extends Role {
  val forsyth = 'k'
  val dirs: List[Direction] = Queen.dirs
  override val attacker = false
}
case object Queen extends PromotableRole {
  val forsyth = 'q'
  val dirs: List[Direction] = Rook.dirs ::: Bishop.dirs
  override val projection = true
}
case object Rook extends PromotableRole {
  val forsyth = 'r'
  val dirs: List[Direction] = List(_.up, _.down, _.left, _.right)
  override val projection = true
}
case object Bishop extends PromotableRole {
  val forsyth = 'b'
  val dirs: List[Direction] = List(_.upLeft, _.upRight, _.downLeft, _.downRight)
  override val projection = true
}
case object Knight extends PromotableRole {
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

  val all: List[Role] = List(King, Queen, Rook, Bishop, Knight, Pawn)
  val allPromotable: List[PromotableRole] = List(Queen, Rook, Bishop, Knight)
  val allByForsyth: Map[Char, Role] = all map { r ⇒ (r.forsyth, r) } toMap
  val allByPgn: Map[Char, Role] = all map { r ⇒ (r.pgn, r) } toMap
  val allPromotableByName: Map[String, PromotableRole] =
    allPromotable map { r ⇒ (r.toString, r) } toMap
  val allPromotableByForsyth: Map[Char, PromotableRole] =
    allPromotable map { r ⇒ (r.forsyth, r) } toMap
  val allPromotableByPgn: Map[Char, PromotableRole] =
    allPromotable map { r ⇒ (r.pgn, r) } toMap

  def forsyth(c: Char): Option[Role] = allByForsyth get c

  def promotable(c: Char): Option[PromotableRole] =
    allPromotableByForsyth get c

  def promotable(name: String): Option[PromotableRole] =
    allPromotableByName get name.capitalize

  def promotable(name: Option[String]): Option[PromotableRole] =
    name flatMap promotable 
}
