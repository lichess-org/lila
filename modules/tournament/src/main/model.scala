package lila.tournament

case class MiniStanding(
  tour: Tournament,
  standing: Option[RankedPlayers])

case class PlayerInfo(rank: Int, withdraw: Boolean) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}

case class VisibleTournaments(
  created: List[Tournament],
  started: List[Tournament],
  finished: List[Tournament])

case class PlayerInfoExt(
  tour: Tournament,
  user: lila.user.User,
  player: Player,
  recentPovs: List[lila.game.Pov])

case class TourAndRanks(
  tour: Tournament,
  whiteRank: Int,
  blackRank: Int)

case class RankedPairing(pairing: Pairing, rank1: Int, rank2: Int) {

  def bestRank = rank1 min rank2
  def rankSum = rank1 + rank2

  def bestColor = chess.Color(rank1 < rank2)
}

object RankedPairing {

  def apply(ranking: Ranking)(pairing: Pairing): Option[RankedPairing] = for {
    r1 <- ranking get pairing.user1
    r2 <- ranking get pairing.user2
  } yield RankedPairing(pairing, r1 + 1, r2 + 1)
}

case class FeaturedGame(
  game: lila.game.Game,
  rankedPairing: RankedPairing)
