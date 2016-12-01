package lila.pool

import lila.common.WMMatching

object MatchMaking {

  case class Couple(p1: PoolMember, p2: PoolMember) {
    def members = Vector(p1, p2)
    def userIds = members.map(_.userId)
    def ratingDiff = p1 ratingDiff p2
  }

  def apply(members: Vector[PoolMember]): Vector[Couple] =
    wmMatching(members) | naive(members)

  private def naive(members: Vector[PoolMember]): Vector[Couple] =
    members.sortBy(-_.rating) grouped 2 collect {
      case Vector(p1, p2) => Couple(p1, p2)
    } toVector

  private object wmMatching {

    // above that, no pairing is allowed
    private val MaxScore = 300

    // quality of a potential pairing. Lower is better.
    private def pairScore(a: PoolMember, b: PoolMember): Int =
      a.ratingDiff(b) - missBonus(a) - missBonus(b)

    // score bonus based on how many waves the member missed
    private def missBonus(p: PoolMember): Int = p.misses * 30

    def apply(members: Vector[PoolMember]): Option[Vector[Couple]] = {
      WMMatching(members.toArray, pairScore).fold(
        err => {
          logger.error("WMMatching", err)
          none
        },
        _.collect {
          case (a, b) if pairScore(a, b).pp < MaxScore => Couple(a, b)
        }.toVector.some
      )
    }
  }
}
