package lila.pool

object MatchMaking {

  case class Pairing(p1: PoolMember, p2: PoolMember) {
    def members = Vector(p1, p2)
  }

  def apply(members: Vector[PoolMember]): Vector[Pairing] =
    members.sortBy(-_.rating) grouped 2 collect {
      case Vector(p1, p2) => Pairing(p1, p2)
    } toVector
}
