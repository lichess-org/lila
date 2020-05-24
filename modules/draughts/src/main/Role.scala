package draughts

sealed trait Role {
  val forsyth: Char
  lazy val pdn: Char = forsyth
  lazy val name: String = toString.toLowerCase
}

sealed trait PromotableRole extends Role

case object King extends PromotableRole {
  val forsyth = 'K'
}

case object Man extends Role {
  val forsyth = ' '
}

case object GhostMan extends Role {
  val forsyth = 'G'
}

case object GhostKing extends Role {
  val forsyth = 'P'
}

object Role {

  val all: List[Role] = List(King, Man)
  val allPromotable: List[PromotableRole] = List(King)

  val allByForsyth: Map[Char, Role] = all.map(r => (r.forsyth, r)).toMap
  val allByPdn: Map[Char, Role] = all.map(r => (r.pdn, r)).toMap
  val allByName: Map[String, Role] = all.map(r => (r.name, r))toMap
  val allPromotableByName: Map[String, PromotableRole] = allPromotable.map(r => (r.toString, r))toMap
  val allPromotableByForsyth: Map[Char, PromotableRole] = allPromotable.map(r => (r.forsyth, r))toMap

  def forsyth(c: Char): Option[Role] = allByForsyth get c

  def promotable(c: Char): Option[PromotableRole] =
    allPromotableByForsyth get c

  def promotable(name: String): Option[PromotableRole] =
    allPromotableByName get name.capitalize

  def promotable(name: Option[String]): Option[PromotableRole] =
    name flatMap promotable

  def valueOf(r: Role): Option[Int] = r match {
    case Man => Some(1)
    case King => Some(2)
    case _ => Some(0)
  }

}
