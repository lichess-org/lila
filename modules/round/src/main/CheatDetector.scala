package lila.round

import akka.actor._

import lila.game.{ Game, GameRepo, Pov }

private[round] final class CheatDetector(meddler: Meddler) {

  def apply(game: Game): Funit = interresting(game) ?? {
    GameRepo findMirror game map {
      _ foreach { mirror ⇒
        def playerUsingAi = mirror.hasAi ?? mirror.players find (_.isHuman)
        def playerByIds = mirror.players find (p ⇒ p.userId ?? game.userIds.contains)
        playerUsingAi orElse playerByIds foreach { p ⇒
          meddler finishCheat Pov(game, !p.color)
        }
      }
    }
  }

  private val TURNS = 12

  private def interresting(game: Game) = game.turns == TURNS && game.rated 
}
