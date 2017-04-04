package lila.tournament

// based on https://en.wikipedia.org/wiki/Elo_rating_system#Performance_rating
private final class Performance {

  private val DIFF = 500

  def apply(tour: Tournament, player: Player, sheet: ScoreSheet): Fu[Option[Int]] =
    if (!tour.isFinished || sheet.scores.size < 3 || player.performance.isDefined) fuccess(player.performance)
    else PairingRepo.finishedByPlayerChronological(tour.id, player.userId) flatMap { pairings =>
      val opponentIds = pairings.flatMap(_ opponentOf player.userId).distinct
      PlayerRepo.byTourAndUserIds(tour.id, opponentIds) flatMap { opponents =>
        val ratingMap: Map[lila.user.User.ID, Int] =
          opponents.map { o => o.userId -> o.finalRating }(scala.collection.breakOut)
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
