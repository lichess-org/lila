package lila
package round

import http.Context
import game.Pov
import templating.ConfigHelper

import com.codahale.jerkson.Json
import scala.math.{ min, max, round }

trait RoundHelper { self: ConfigHelper ⇒

  def roundPlayerJsData(pov: Pov, version: Int) = Json generate {

    import pov._

    Map(
      "game" -> Map(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.lastMove
      ),
      "player" -> Map(
        "id" -> player.id,
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> false
      ),
      "opponent" -> Map(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi
      ),
      "possible_moves" -> possibleMoves(pov),
      "animation_delay" -> animationDelay(pov)
    )
  }

  def roundWatcherJsData(pov: Pov, version: Int) = Json generate {

    import pov._

    Map(
      "game" -> Map(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.lastMove
      ),
      "player" -> Map(
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> true
      ),
      "opponent" -> Map(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi
      ),
      "possible_moves" -> possibleMoves(pov),
      "animation_delay" -> animationDelay(pov)
    )
  }

  private def possibleMoves(pov: Pov) = (pov.game playableBy pov.player) option {
    pov.game.toChess.situation.destinations map {
      case (from, dests) ⇒ from.key -> (dests.mkString)
    } toMap
  }

  private def animationDelay(pov: Pov) = round {
    gameAnimationDelay * max(0, min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
  }
}
