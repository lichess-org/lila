package lila.pool

object MatchMaking {

  case class Couple(p1: PoolMember, p2: PoolMember) {
    def members = Vector(p1, p2)
  }

  def apply(members: Vector[PoolMember]): Vector[Couple] =
    members.sortBy(-_.rating) grouped 2 collect {
      case Vector(p1, p2) => Couple(p1, p2)
    } toVector
}
