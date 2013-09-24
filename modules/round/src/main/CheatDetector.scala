package lila.round

import akka.actor._
import chess.Color

import lila.game.{ Game, GameRepo, Pov }

private[round] final class CheatDetector {

  def apply(game: Game): Fu[Option[Color]] = interresting(game) ?? {
    GameRepo findMirror game map {
      _ ?? { mirror ⇒
        def playerUsingAi = mirror.hasAi ?? mirror.players find (_.isHuman)
        def playerByIds = mirror.players find (p ⇒ p.userId ?? game.userIds.contains)
        (playerUsingAi orElse playerByIds) map (!_.color)
      }
    }
  }

  private val TURNS = 12

  private def interresting(game: Game) = game.turns == TURNS && game.rated
}
