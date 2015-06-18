package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }

import scala.util.Random

object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  case class Data(
    tour: Tournament,
    recentPairings: List[Pairing],
    ranking: Map[String, Int],
    nbActiveUsers: Int)

  // if waiting users can make pairings
  // then pair all users
  def createPairings(
    tour: Tournament,
    users: WaitingUsers): Fu[(Pairings, Events)] = for {
    recentPairings <- PairingRepo.recentByTourAndUserIds(tour.id, users.all, Math.min(120, users.size * 5))
    nbActiveUsers <- PlayerRepo.countActive(tour.id)
    ranking <- PlayerRepo.ranking(tour.id)
    data = Data(tour, recentPairings, ranking, nbActiveUsers)
    pairings <- {
      if (recentPairings.isEmpty) evenOrAll(data, users)
      else tryPairings(data, users.waiting) flatMap {
        case Nil => fuccess(Nil)
        case _   => evenOrAll(data, users)
      }
    }
  } yield {
    if (pairings.nonEmpty && pairings.size * 2 < users.size) {
      val left = users.all diff pairings.flatMap(_.users) map { id =>
        val waitSeconds = users waitSecondsOf id
        s"$id [${ranking.get(id).fold(0)(1+)}] ${waitSeconds getOrElse "?"}s"
      }
      if (left.nonEmpty) println(s"[arena ${tour.id} $nbActiveUsers/${tour.nbPlayers}] left over: ${left mkString ", "} out of ${users.size}")
    }
    pairings -> Nil
  }

  private def evenOrAll(data: Data, users: WaitingUsers) =
    tryPairings(data, users.evenNumber) flatMap {
      case Nil if users.isOdd => tryPairings(data, users.all)
      case x                  => fuccess(x)
    }

  val smartHardLimit = 24

  private def tryPairings(data: Data, users: List[String]): Fu[Pairings] = {
    import data._
    if (users.size < 2) fuccess(Nil)
    else PlayerRepo.rankedByTourAndUserIds(tour.id, users, ranking) map { idles =>
      if (recentPairings.isEmpty) naivePairings(tour, idles)
      else
        smartPairings(data, idles take smartHardLimit) :::
          naivePairings(tour, idles drop smartHardLimit)
    }
  }

  private def naivePairings(tour: Tournament, players: RankedPlayers) =
    players grouped 2 collect {
      case List(p1, p2) if Random.nextBoolean => Pairing(tour, p1.player, p2.player)
      case List(p1, p2)                       => Pairing(tour, p2.player, p1.player)
    } toList

  private def smartPairings(data: Data, players: RankedPlayers): Pairings = {
    import data._

    type Score = Int
    type RankedPairing = (RankedPlayer, RankedPlayer)
    type Combination = List[RankedPairing]

    val lastOpponents: Map[String, String] = players.flatMap { p =>
      recentPairings.find(_ contains p.player.userId).flatMap(_ opponentOf p.player.userId) map {
        p.player.userId -> _
      }
    }.toMap

    def justPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.get(u1).exists(u2==) || lastOpponents.get(u2).exists(u1==)

    def veryMuchJustPlayedTogether(u1: String, u2: String): Boolean =
      lastOpponents.get(u1).exists(u2==) && lastOpponents.get(u2).exists(u1==)

    // lower is better
    def pairingScore(pair: RankedPairing): Score = pair match {
      case (a, b) => Math.abs(a.rank - b.rank) * 1000 +
        Math.abs(a.player.rating - b.player.rating) +
        justPlayedTogether(a.player.userId, b.player.userId).?? {
          if (veryMuchJustPlayedTogether(a.player.userId, b.player.userId)) 9000 * 1000
          else 8000 * 1000
        }
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
      recentPairings.find(_.contains(p1.userId, p2.userId)) match {
        case Some(p) => p.user1 != p1.userId
        case None    => Random.nextBoolean
      }

    (players match {
      case x if x.size < 2 => Nil
      case List(p1, p2) if nbActiveUsers == 2 =>
        if (firstPlayerGetsWhite(p1.player, p2.player)) List(p1.player -> p2.player)
        else List(p2.player -> p1.player)
      case List(p1, p2) if justPlayedTogether(p1.player.userId, p2.player.userId) => Nil
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
    }) map {
      Pairing(tour, _)
    }
  }
}
