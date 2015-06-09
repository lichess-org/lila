package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

import scala.concurrent.Future
import scala.util.Random

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  // if waiting users can make pairings
  // then pair all users
  def createPairings(tour: Tournament, users: AllUserIds): Future[(Pairings, Events)] =
    tryPairings(tour, users.waiting) flatMap {
      case (Nil, _) if tour.pairings.isEmpty => tryPairings(tour, users.all)
      case nope@(Nil, _)                     => fuccess(nope)
      case _                                 => tryPairings(tour, users.all)
    }

  val smartHardLimit = 24

  private def tryPairings(tour: Tournament, users: List[String]): Future[(Pairings, Events)] = {
    val nbActiveUsers = tour.nbActiveUsers

    if (users.size < 2)
      Future.successful((Nil, Nil))
    else {
      val idles: RankedPlayers = users.toSet.diff {
        tour.playingUserIds.toSet
      }.toList flatMap tour.rankedPlayerByUserId sortBy { p =>
        p.rank -> -p.player.rating
      }

      val ps =
        if (tour.pairings.isEmpty) naivePairings(idles)
        else
          smartPairings(idles take smartHardLimit, tour.pairings, nbActiveUsers) :::
            naivePairings(idles drop smartHardLimit)

      Future.successful((ps, Nil))
    }
  }

  private def naivePairings(players: RankedPlayers) =
    players grouped 2 collect {
      case List(p1, p2) if Random.nextBoolean => Pairing(p1.player, p2.player)
      case List(p1, p2)                       => Pairing(p2.player, p1.player)
    } toList

  private def smartPairings(players: RankedPlayers, pairings: Pairings, nbActiveUsers: Int): Pairings = {

    type Score = Int
    type RankedPairing = (RankedPlayer, RankedPlayer)
    type Combination = List[RankedPairing]

    val lastOpponents: Map[String, String] = players.flatMap { p =>
      pairings.find(_ contains p.player.id).flatMap(_ opponentOf p.player.id) map {
        p.player.id -> _
      }
    }.toMap

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.get(u1).exists(u2==) || lastOpponents.get(u2).exists(u1==)

    def veryMuchJustPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.get(u1).exists(u2==) && lastOpponents.get(u2).exists(u1==)

    // lower is better
    def pairingScore(pair: RankedPairing): Score = pair match {
      case (a, b) if justPlayedTogether(a.player.id, b.player.id) =>
        if (veryMuchJustPlayedTogether(a.player.id, b.player.id)) 9000 * 1000
        else 8000 * 1000
      case (a, b) => Math.abs(a.rank - b.rank) * 1000 +
        Math.abs(a.player.rating - b.player.rating)
    }
    def score(pairs: Combination): Score = pairs.foldLeft(0) {
      case (s, p) => s + pairingScore(p)
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
            else findBetter(next, toBeat) match {
              case Found(b) => b.some
              case End      => next.some
              case NoBetter => current
            }
        } match {
          case Some(best) => Found(best)
          case None       => NoBetter
        }
      }

    def firstPlayerGetsWhite(p1: Player, p2: Player) =
      pairings.find(_.contains(p1.id, p2.id)) match {
        case Some(p) => p.user1 != p1.id
        case None    => Random.nextBoolean
      }

    (players match {
      case x if x.size < 2 => Nil
      case List(p1, p2) if nbActiveUsers == 2 => List(p1.player -> p2.player)
      case List(p1, p2) if justPlayedTogether(p1.player.id, p2.player.id) => Nil
      case List(p1, p2) => List(p1.player -> p2.player)
      case ps => findBetter(Nil, Int.MaxValue) match {
        case Found(best) => best map {
          case (rp0, rp1) if Random.nextBoolean => rp0.player -> rp1.player
          case (rp0, rp1)                       => rp1.player -> rp0.player
        }
        case _ =>
          logwarn("Could not make smart pairings for arena tournament")
          players map (_.player) grouped 2 collect {
            case List(p1, p2) if firstPlayerGetsWhite(p1, p2) => (p1, p2)
            case List(p1, p2)                                 => (p2, p1)
          } toList
      }
    }) map Pairing.apply
  }
}
