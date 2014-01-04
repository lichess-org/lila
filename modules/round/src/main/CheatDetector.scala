package lila.round

import akka.actor._
import chess.Color

import lila.game.{ Game, GameRepo, Pov }

private[round] final class CheatDetector {

  def apply(game: Game): Fu[Option[Color]] = interresting(game) ?? {
    GameRepo findMirror game map {
      _ ?? { mirror ⇒
        mirror.players find (p ⇒ p.userId ?? game.userIds.contains) match {
          case Some(player) ⇒ {
            val color = !player.color
            play.api.Logger("cheat detector").info(s"$color @ ${game.id} uses ${mirror.id}")
            Some(color)
          }
          case None ⇒ None
        }
      }
    }
  }

  private val TURNS_MODULUS = 10

  private def interresting(game: Game) =
    game.rated && game.turns > 0 && (game.turns % TURNS_MODULUS == 0)
}
