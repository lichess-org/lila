package lila.tournament

// based on https://en.wikipedia.org/wiki/Elo_rating_system#Performance_rating
private final class Performance {

  private val DIFF = 500

  def apply(tour: Tournament, player: Player, pairings: Pairings): Fu[Option[Int]] =
    if (!tour.isFinished || pairings.size < 3 || player.performance.isDefined) fuccess(player.performance)
    else {
      val opponentIds = pairings.flatMap(_ opponentOf player.userId).distinct
      PlayerRepo.byTourAndUserIds(tour.id, opponentIds) flatMap { opponents =>
        val ratingMap = opponents.map { o => o.userId -> o.finalRating }.toMap
        val performance = pairings.foldLeft(0) {
          case (acc, pairing) => acc +
            ~(pairing.opponentOf(player.userId) flatMap ratingMap.get) + {
              if (pairing wonBy player.userId) DIFF
              else if (pairing lostBy player.userId) -DIFF
              else 0
            }
        } / pairings.size
        PlayerRepo.setPerformance(player, performance) inject performance.some
      }
    }
}
