package lila.tournament
package arena

import PairingSystem.{ Data, url }

private object OrnicarPairing {

  private val smartPairingsMaxMillis = 600

  def apply(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.nonEmpty ?? {
    import data._

    val startAt = nowMillis
    val stopAt = startAt + smartPairingsMaxMillis
    def continue = nowMillis < stopAt

    type Score = Int
    type RankedPairing = (RankedPlayer, RankedPlayer)
    type Combination = List[RankedPairing]

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.hash.get(u1).contains(u2) || lastOpponents.hash.get(u2).contains(u1)

    def veryMuchJustPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.hash.get(u1).contains(u2) && lastOpponents.hash.get(u2).contains(u1)

    def rankFactor = PairingSystem.rankFactorFor(players)

    // optimized for speed
    def score(pairs: Combination): Score = {
      var i = 0
      pairs.foreach {
        case (a, b) =>
          // lower is better
          i = i + Math.abs(a.rank - b.rank) * rankFactor(a, b) +
            Math.abs(a.player.rating - b.player.rating) +
            justPlayedTogether(a.player.userId, b.player.userId).?? {
              if (veryMuchJustPlayedTogether(a.player.userId, b.player.userId)) 9000 * 1000
              else 8000 * 1000
            }
      }
      i
    }

    def nextCombos(combo: Combination): List[Combination] =
      players.filterNot { p =>
        combo.exists(c => c._1 == p || c._2 == p)
      } match {
        case a :: rest => rest.map { b =>
          (a, b) :: combo
        }
        case _ => Nil
      }

    sealed trait FindBetter
    case class Found(best: Combination) extends FindBetter
    case object End extends FindBetter
    case object NoBetter extends FindBetter

    def findBetter(from: Combination, than: Score): FindBetter =
      nextCombos(from) match {
        case Nil => End
        case nexts => nexts.foldLeft(none[Combination]) {
          case (current, next) =>
            val toBeat = current.fold(than)(score)
            if (score(next) >= toBeat) current
            else if (continue) findBetter(next, toBeat) match {
              case Found(b) => b.some
              case End => next.some
              case NoBetter => current
            }
            else current
        } match {
          case Some(best) => Found(best)
          case None => NoBetter
        }
      }

    val preps = (players match {
      case x if x.size < 2 => Nil
      case List(p1, p2) if onlyTwoActivePlayers => List(p1.player -> p2.player)
      case List(p1, p2) if justPlayedTogether(p1.player.userId, p2.player.userId) => Nil
      case List(p1, p2) => List(p1.player -> p2.player)
      case ps => findBetter(Nil, Int.MaxValue) match {
        case Found(best) => best map {
          case (rp0, rp1) => rp0.player -> rp1.player
        }
        case _ =>
          pairingLogger.warn("Could not make smart pairings for arena tournament")
          players map (_.player) grouped 2 collect {
            case List(p1, p2) => (p1, p2)
          } toList
      }
    }) map {
      Pairing.prep(tour, _)
    }
    if (!continue) {
      pairingLogger.info(s"smartPairings cutoff! [${nowMillis - startAt}ms] ${url(data.tour.id)} ${players.size} players, ${preps.size} preps")
      lila.mon.tournament.pairing.cutoff()
    }
    preps
  }
}
