package lila.game

import chess.Speed
import lila.user.{ User, UserRepo, SpeedElos, SpeedElo }

private[game] final class ComputeElos {

  private lazy val eloCalculator = new chess.EloCalculator(false)

  def apply(user: User): Funit = GameRepo recentFinishedByUser user.id map {
    _.foldLeft(SpeedElos.default) {
      case (elos, game) ⇒ (for {
        player ← game player user
        opponentElo ← game.opponent(player).elo
      } yield {
        val speed = Speed(game.clock)
        val speedElo = user.speedElos(speed)
        val opponentSpeedElo = SpeedElo(0, opponentElo)
        val (white, black) = player.color.fold[(eloCalculator.User, eloCalculator.User)](
          speedElo -> opponentSpeedElo,
          opponentSpeedElo -> speedElo)
        val newElos = eloCalculator.calculate(white, black, game.winnerColor)
        val newElo = player.color.fold(newElos._1, newElos._2)
        elos.addGame(speed, newElo)
      }) | elos
    }
  } void

}
