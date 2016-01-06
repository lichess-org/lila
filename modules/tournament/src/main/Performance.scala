package lila.tournament

// based on https://en.wikipedia.org/wiki/Elo_rating_system#Performance_rating
private final class Performance {

  private val DIFF = 500

  def apply(tour: Tournament, player: Player, pairings: Pairings): Fu[Option[Int]] =
    if (!tour.isFinished || pairings.size < 3 || player.performance.isDefined) fuccess(player.performance)
    else {
      val opponentIds = pairings.flatMap(_ opponentOf player.userId).distinct
      PlayerRepo.byTourAndUserIds(tour.id, opponentIds) flatMap { opponents =>
        val meanRating = opponents.map(_.finalRating).sum
        val wins = pairings.count(_ wonBy player.userId)
        val losses = pairings.count(_ lostBy player.userId)
        val totalDiff = DIFF * (wins - losses)
        val performance = (meanRating + totalDiff) / pairings.size
        PlayerRepo.setPerformance(player, performance) inject performance.some
      }
    }
}
