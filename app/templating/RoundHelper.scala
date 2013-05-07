package lila.app
package templating

import lila.user.Context
import lila.game.Pov
import lila.round.Env.{ current ⇒ roundEnv }

import play.api.libs.json.Json
import scala.math.{ min, max, round }

trait RoundHelper {

  def moretimeSeconds = roundEnv.moretimeSeconds

  def gameAnimationDelay = roundEnv.animationDelay

  def roundPlayerJsData(pov: Pov, version: Int) = {
    import pov._
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.lastMove
      ),
      "player" -> Json.obj(
        "id" -> player.id,
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> false
      ),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi
      ),
      "possible_moves" -> possibleMoves(pov),
      "animation_delay" -> animationDelay(pov),
      "tournament_id" -> game.tournamentId
    )
  }

  def roundWatcherJsData(pov: Pov, version: Int) = {
    import pov._
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "lastMove" -> game.lastMove
      ),
      "player" -> Json.obj(
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> true
      ),
      "opponent" -> Json.obj(
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
    roundEnv.animationDelay.toMillis *
      max(0, min(1.2,
        ((pov.game.estimateTotalTime - 60) / 60) * 0.2
      ))
  }
}
