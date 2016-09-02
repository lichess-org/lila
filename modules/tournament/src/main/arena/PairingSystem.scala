package lila.tournament
package arena

import lila.tournament.{ PairingSystem => AbstractPairingSystem }
import lila.user.UserRepo

import scala.util.Random

private[tournament] object PairingSystem extends AbstractPairingSystem {
  type P = (String, String)

  case class Data(
      tour: Tournament,
      lastOpponents: Pairing.LastOpponents,
      ranking: Map[String, Int],
      onlyTwoActivePlayers: Boolean) {

    val isFirstRound = lastOpponents.hash.isEmpty && tour.isRecentlyStarted
  }

  // if waiting users can make pairings
  // then pair all users
  def createPairings(tour: Tournament, users: WaitingUsers, ranking: Ranking): Fu[Pairings] = {
    for {
      lastOpponents <- PairingRepo.lastOpponents(tour.id, users.all, Math.min(100, users.size * 4))
      onlyTwoActivePlayers <- (tour.nbPlayers > 20).fold(
        fuccess(false),
        PlayerRepo.countActive(tour.id).map(2==))
      data = Data(tour, lastOpponents, ranking, onlyTwoActivePlayers)
      preps <- if (data.isFirstRound) evenOrAll(data, users)
      else makePreps(data, users.waiting) flatMap {
        case Nil => fuccess(Nil)
        case _   => evenOrAll(data, users)
      }
      pairings <- prepsToPairings(preps)
    } yield pairings
  }.chronometer.logIfSlow(500, pairingLogger) { pairings =>
    s"createPairings ${url(tour.id)} ${pairings.size} pairings"
  }.result

  private def evenOrAll(data: Data, users: WaitingUsers) =
    makePreps(data, users.evenNumber) flatMap {
      case Nil if users.isOdd => makePreps(data, users.all)
      case x                  => fuccess(x)
    }

  val pairingGroupSize = 30

  private def makePreps(data: Data, users: List[String]): Fu[List[Pairing.Prep]] = {
    import data._
    if (users.size < 2) fuccess(Nil)
    else PlayerRepo.rankedByTourAndUserIds(tour.id, users, ranking) map { idles =>
      if (data.tour.isRecentlyStarted) naivePairings(tour, idles)
      else idles.grouped(pairingGroupSize).toList match {
        case a :: b :: c :: _ => smartPairings(data, a) ::: smartPairings(data, b) ::: smartPairings(tour, c take pairingGroupSize)
        case a :: b :: Nil    => smartPairings(data, a) ::: smartPairings(data, b)
        case a :: Nil         => smartPairings(data, a)
        case Nil              => Nil
      }
    }
  }.chronometer.mon(_.tournament.pairing.prepTime).logIfSlow(200, pairingLogger) { preps =>
    s"makePreps ${url(data.tour.id)} ${users.size} users, ${preps.size} preps"
  }.result

  private def prepsToPairings(preps: List[Pairing.Prep]): Fu[List[Pairing]] =
    if (preps.size < 50) preps.map { prep =>
      UserRepo.firstGetsWhite(prep.user1.some, prep.user2.some) map prep.toPairing
    }.sequenceFu
    else fuccess {
      preps.map(_ toPairing Random.nextBoolean)
    }

  private def naivePairings(tour: Tournament, players: RankedPlayers): List[Pairing.Prep] =
    players grouped 2 collect {
      case List(p1, p2) => Pairing.prep(tour, p1.player, p2.player)
    } toList

  private def smartPairings(data: Data, players: RankedPlayers): List[Pairing.Prep] = players.size match {
    case x if x < 2   => Nil
    case x if x <= 10 => OrnicarPairing(data, players)
    case _            => AntmaPairing(data, players)
  }

  private[arena] def url(tourId: String) = s"https://lichess.org/tournament/$tourId"
}
