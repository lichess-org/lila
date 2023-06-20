package lila.pool

import scala.math.abs

import lila.common.WMMatching

object MatchMaking:

  case class Couple(p1: PoolMember, p2: PoolMember):
    def members    = Vector(p1, p2)
    def userIds    = members.map(_.userId)
    def ratingDiff = p1 ratingDiff p2

  def apply(members: Vector[PoolMember]): Vector[Couple] =
    members.partition(_.lame) match
      case (lames, fairs) => naive(lames) ++ (wmMatching(fairs) | naive(fairs))

  private def naive(members: Vector[PoolMember]): Vector[Couple] =
    members.sortBy(_.rating)(using intOrdering[IntRating].reverse) grouped 2 collect { case Vector(p1, p2) =>
      Couple(p1, p2)
    } toVector

  private object wmMatching:

    // above that, no pairing is allowed
    // 1000 ~> 130
    // 1200 ~> 100
    // 1500 ~> 100
    // 2000 ~> 133
    // 2500 ~> 166
    // 3000 ~> 200
    private def ratingToMaxScore(rating: IntRating) =
      if (rating < 1000) 130
      else if (rating < 1500) 100
      else rating.value / 15

    // quality of a potential pairing. Lower is better.
    // None indicates a forbidden pairing
    private def pairScore(a: PoolMember, b: PoolMember): Option[Int] =
      ! {
        ratingRangeConflict(a, b) ||
        ratingRangeConflict(b, a) ||
        blockList(a, b) ||
        blockList(b, a)
      } so {
        a.ratingDiff(b).value
          - missBonus(a).atMost(missBonus(b))
          - rangeBonus(a, b)
          - ragesitBonus(a, b)

      }.some.filter(score => score <= ratingToMaxScore(a.rating atMost b.rating))

    // score bonus based on how many waves the member missed
    // when the user's sit counter is lower than -3, the maximum bonus becomes lower
    private def missBonus(p: PoolMember) =
      (p.misses * 12) atMost ((460 + (p.rageSitCounter atMost -3) * 20) atLeast 0)

    // if players have conflicting rating ranges
    private def ratingRangeConflict(a: PoolMember, b: PoolMember): Boolean =
      a.ratingRange.exists(!_.contains(b.rating))

    // bonus if both players have rating ranges, and they're compatible
    private def rangeBonus(a: PoolMember, b: PoolMember) =
      if (a.ratingRange.exists(_ contains b.rating) && b.ratingRange.exists(_ contains a.rating)) 200
      else 0

    // if players block each other
    private def blockList(a: PoolMember, b: PoolMember): Boolean =
      a.blocking.value contains b.userId

    // bonus if the two players both have a good sit counter
    // bonus if the two players both have a bad sit counter
    // malus (so negative number as bonus) if neither of those are true, meaning that their sit counters are far away (e.g. 0 and -5)
    private def ragesitBonus(a: PoolMember, b: PoolMember) =
      if (a.rageSitCounter >= -2 && b.rageSitCounter >= -2) 30        // good players
      else if (a.rageSitCounter <= -12 && b.rageSitCounter <= -12) 60 // very bad players
      else if (a.rageSitCounter <= -5 && b.rageSitCounter <= -5) 30   // bad players
      else (abs(a.rageSitCounter - b.rageSitCounter) atMost 10) * -20 // match of good and bad player

    def apply(members: Vector[PoolMember]): Option[Vector[Couple]] =
      WMMatching(members.toArray, pairScore).fold(
        err => {
          logger.error("WMMatching", err)
          none
        },
        pairs =>
          Some {
            pairs.view.map { case (a, b) => Couple(a, b) } to Vector
          }
      )
