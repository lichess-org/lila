package lila.round

import akka.actor._
import chess.Color

import lila.game.{ Game, GameRepo, Pov }

private[round] final class CheatDetector(reporter: ActorSelection) {

  def apply(game: Game): Fu[Option[Color]] = interesting(game) ?? {
    GameRepo findMirror game map {
      _ ?? { mirror ⇒
        mirror.players find (p ⇒ p.userId ?? game.userIds.contains) match {
          case Some(player) ⇒
            val color = !player.color
            play.api.Logger("cheat detector").info(s"$color @ ${game.id} uses ${mirror.id}")
            player.userId foreach { userId ⇒
              reporter ! lila.hub.actorApi.report.Cheater(userId,
                s"Cheat detected on ${gameUrl(game.id)}, using lichess AI: ${gameUrl(mirror.id)}")
            }
            Some(color)
          case None ⇒ None
        }
      }
    }
  }

  private def gameUrl(gameId: String) = s"http://lichess.org/${gameId}"

  private val TURNS_MODULUS = 10

  private def interesting(game: Game) =
    game.rated && game.turns > 0 && (game.turns % TURNS_MODULUS == 0)
}
