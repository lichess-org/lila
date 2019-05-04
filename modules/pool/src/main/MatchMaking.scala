package lila.pool

import lila.common.WMMatching

object MatchMaking {

  case class Couple(p1: PoolMember, p2: PoolMember) {
    def members = Vector(p1, p2)
    def userIds = members.map(_.userId)
    def ratingDiff = p1 ratingDiff p2
  }

  def apply(members: Vector[PoolMember]): Vector[Couple] = members.partition(_.lame) match {
    case (lames, fairs) => naive(lames) ++ (wmMatching(fairs) | naive(fairs))
  }

  private def naive(members: Vector[PoolMember]): Vector[Couple] =
    members.sortBy(-_.rating) grouped 2 collect {
      case Vector(p1, p2) => Couple(p1, p2)
    } toVector

  private object wmMatching {

    // above that, no pairing is allowed
    // 1800 ~> 300
    // 2500 ~> 450
    // 3000 ~> 650
    private def ratingToMaxScore(rating: Int) =
      if (rating < 2000) 300
      else rating / 3.5 - 250

    // quality of a potential pairing. Lower is better.
    // None indicates a forbidden pairing
    private def pairScore(a: PoolMember, b: PoolMember): Option[Int] = {
      val score = a.ratingDiff(b) - {
        missBonus(a) atMost missBonus(b)
      } + {
        rangeMalus(a, b) + rangeMalus(b, a)
      } + {
        blockMalus(a, b) + blockMalus(b, a)
      }
      val maxScore = ratingToMaxScore(a.rating atLeast b.rating)
      if (score <= maxScore) Some(score) else None
    }

    // score bonus based on how many waves the member missed
    private def missBonus(p: PoolMember) = (p.misses * 15) atMost 400

    // big malus if players have conflicting rating ranges
    private def rangeMalus(a: PoolMember, b: PoolMember) =
      if (a.ratingRange.exists(!_.contains(b.rating))) 6000 else 0

    // huge malus if players block each other
    private def blockMalus(a: PoolMember, b: PoolMember) =
      if (a.blocking.ids contains b.userId) 9000 else 0

    def apply(members: Vector[PoolMember]): Option[Vector[Couple]] = {
      WMMatching(members.toArray, pairScore).fold(
        err => {
          logger.error("WMMatching", err)
          none
        },
        pairs => Some {
          pairs.map { case (a, b) => Couple(a, b) }(scala.collection.breakOut)
        }
      )
    }
  }
}
