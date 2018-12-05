package lila.tournament

case class TournamentTop(value: List[Player]) extends AnyVal

case class TourMiniView(tour: Tournament, top: Option[TournamentTop])

case class MyInfo(rank: Int, withdraw: Boolean, gameId: Option[lila.game.Game.ID]) {
  def page = {
    math.floor((rank - 1) / 10) + 1
  }.toInt
}

case class VisibleTournaments(
    created: List[Tournament],
    started: List[Tournament],
    finished: List[Tournament]
) {

  def unfinished = created ::: started

  def all = started ::: created ::: finished
}

case class PlayerInfoExt(
    tour: Tournament,
    user: lila.user.User,
    player: Player,
    recentPovs: List[lila.game.LightPov]
)

case class TourAndRanks(
    tour: Tournament,
    whiteRank: Int,
    blackRank: Int
)

case class RankedPairing(pairing: Pairing, rank1: Int, rank2: Int) {

  def bestRank = rank1 min rank2
  // def rankSum = rank1 + rank2

  def bestColor = chess.Color(rank1 < rank2)
}

object RankedPairing {

  def apply(ranking: Ranking)(pairing: Pairing): Option[RankedPairing] = for {
    r1 <- ranking get pairing.user1
    r2 <- ranking get pairing.user2
  } yield RankedPairing(pairing, r1 + 1, r2 + 1)
}

case class RankedPlayer(rank: Int, player: Player) {

  def is(other: RankedPlayer) = player is other.player

  override def toString = s"$rank. ${player.userId}[${player.rating}]"
}

object RankedPlayer {

  def apply(ranking: Ranking)(player: Player): Option[RankedPlayer] =
    ranking get player.userId map { rank =>
      RankedPlayer(rank + 1, player)
    }
}

case class FeaturedGame(
    game: lila.game.Game,
    white: RankedPlayer,
    black: RankedPlayer
)
