package lila.pool

import lila.common.WMMatching

object MatchMaking {

  case class Couple(p1: PoolMember, p2: PoolMember) {
    def members = Vector(p1, p2)
    def userIds = members.map(_.userId)
  }

  def apply(members: Vector[PoolMember]): Vector[Couple] =
    wmMatching(members) | naive(members)

  private def naive(members: Vector[PoolMember]): Vector[Couple] =
    members.sortBy(-_.rating) grouped 2 collect {
      case Vector(p1, p2) => Couple(p1, p2)
    } toVector

  private def pairScore(a: PoolMember, b: PoolMember): Int =
    Math.abs(a.rating - b.rating)

  private def wmMatching(members: Vector[PoolMember]): Option[Vector[Couple]] = {
    WMMatching(members.toArray, pairScore).fold(
      err => {
        logger.error("WMMatching", err)
        none
      },
      _.map { case (a, b) => Couple(a, b) }.toVector.some
    )
  }
}
