package lila.round

import scala.concurrent.duration._
import scala.math.{ min, max, round }

import play.api.libs.json.Json

import lila.game.{ Pov, Game }
import lila.pref.Pref

final class JsonView(baseAnimationDelay: Duration) {

  def playerJson(pov: Pov, version: Int, pref: Pref) = {
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
        "startedAtTurn" -> game.startedAtTurn,
        "lastMove" -> game.castleLastMoveTime.lastMoveString),
      "player" -> Json.obj(
        "id" -> playerId,
        "color" -> player.color.name,
        "version" -> version,
        "spectator" -> false
      ),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi
      ),
      "url" -> Json.obj(
        "socket" -> s"/$fullId/socket",
        "end" -> s"/$fullId/end",
        "table" -> s"/$fullId/table"
      ),
      "pref" -> Json.obj(
        "animationDelay" -> animationDelay(pov),
        "autoQueen" -> pref.autoQueen,
        "autoThreefold" -> pref.autoThreefold,
        "clockTenths" -> pref.clockTenths,
        "clockBar" -> pref.clockBar,
        "enablePremove" -> pref.premove
      ),
      "possibleMoves" -> possibleMoves(pov),
      "tournamentId" -> game.tournamentId
    )
  }

  def watcherJson(pov: Pov, version: Int, tv: Boolean, pref: Pref) = {
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
        "startedAtTurn" -> game.startedAtTurn,
        "lastMove" -> game.castleLastMoveTime.lastMoveString),
      "player" -> Json.obj(
        "color" -> color.name,
        "version" -> version,
        "spectator" -> true),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.isAi),
      "url" -> Json.obj(
        "socket" -> s"/$gameId/${color.name}/socket",
        "end" -> s"/$gameId/${color.name}/end",
        "table" -> s"/$gameId/${color.name}/table"
      ),
      "pref" -> Json.obj(
        "animationDelay" -> animationDelay(pov),
        "clockTenths" -> pref.clockTenths,
        "clockBar" -> pref.clockBar
      ),
      "possibleMoves" -> possibleMoves(pov),
      "tv" -> tv
    )
  }

  private def possibleMoves(pov: Pov) = (pov.game playableBy pov.player) option {
    pov.game.toChess.situation.destinations map {
      case (from, dests) => from.key -> dests.mkString
    }
  }

  private def animationDelay(pov: Pov) = round {
    baseAnimationDelay.toMillis * max(0, min(1.2,
      ((pov.game.estimateTotalTime - 60) / 60) * 0.2
    ))
  }
}
