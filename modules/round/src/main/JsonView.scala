package lila.round

import scala.concurrent.duration._
import scala.math.{ min, max, round }

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.{ Pov, Game, PerfPicker }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import chess.format.Forsyth
import chess.{ Color, Clock }

final class JsonView(
    chatApi: lila.chat.ChatApi,
    userJsonView: lila.user.JsonView,
    getVersion: String => Fu[Int],
    canTakeback: Game => Fu[Boolean],
    baseAnimationDuration: Duration) {

  def playerJson(
    pov: Pov,
    pref: Pref,
    apiVersion: Int,
    playerUser: Option[User]): Fu[JsObject] =
    getVersion(pov.game.id) zip
      (pov.opponent.userId ?? UserRepo.byId) zip
      canTakeback(pov.game) zip
      getChat(pov.game, playerUser) map {
        case (((version, opponentUser), takebackable), chat) =>
          import pov._
          Json.obj(
            "game" -> Json.obj(
              "id" -> gameId,
              "variant" -> Json.obj(
                "key" -> game.variant.key,
                "name" -> game.variant.name,
                "short" -> game.variant.shortName,
                "title" -> game.variant.title),
              "speed" -> game.speed.key,
              "perf" -> PerfPicker.key(game),
              "rated" -> game.rated,
              "fen" -> (Forsyth >> game.toChess),
              "moves" -> game.pgnMoves.mkString(" "),
              "clock" -> game.hasClock,
              "clockRunning" -> game.isClockRunning,
              "player" -> game.turnColor.name,
              "winner" -> game.winnerColor.map(_.name),
              "turns" -> game.turns,
              "startedAtTurn" -> game.startedAtTurn,
              "lastMove" -> game.castleLastMoveTime.lastMoveString,
              "status" -> Json.obj(
                "id" -> pov.game.status.id,
                "name" -> pov.game.status.name)),
            "clock" -> game.clock.map(clockJson),
            "player" -> Json.obj(
              "id" -> playerId,
              "color" -> player.color.name,
              "version" -> version,
              "spectator" -> false,
              "user" -> playerUser.map { userJsonView(_, true) },
              "isOfferingRematch" -> player.isOfferingRematch.option(true),
              "isOfferingDraw" -> player.isOfferingDraw.option(true),
              "isProposingTakeback" -> player.isProposingTakeback.option(true)
            ).noNull,
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView(_, true) },
              "isOfferingRematch" -> opponent.isOfferingRematch.option(true),
              "isOfferingDraw" -> opponent.isOfferingDraw.option(true),
              "isProposingTakeback" -> opponent.isProposingTakeback.option(true)
            ).noNull,
            "url" -> Json.obj(
              "socket" -> s"/$fullId/socket/v$apiVersion",
              "round" -> s"/$fullId"
            ),
            "pref" -> Json.obj(
              "animationDuration" -> animationDuration(pov, pref),
              "highlight" -> pref.highlight,
              "destination" -> pref.destination,
              "autoQueen" -> pref.autoQueen,
              "autoThreefold" -> pref.autoThreefold,
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "enablePremove" -> pref.premove
            ),
            "chat" -> chat.map { c =>
              JsArray(c.lines map {
                case lila.chat.UserLine(username, text, _) => Json.obj(
                  "u" -> username,
                  "t" -> text)
                case lila.chat.PlayerLine(color, text) => Json.obj(
                  "c" -> color.name,
                  "t" -> text)
              })
            },
            "possibleMoves" -> possibleMoves(pov),
            "pool" -> game.poolId.map { pid =>
              Json.obj(
                "id" -> pid
              )
            },
            "takebackable" -> takebackable)
      }

  def watcherJson(pov: Pov, version: Int, tv: Boolean, pref: Pref) = {
    import pov._
    Json.obj(
      "game" -> Json.obj(
        "id" -> gameId,
        "variant" -> game.variant.key,
        "speed" -> game.speed.key,
        "perf" -> PerfPicker.key(game),
        "rated" -> game.rated,
        "started" -> game.started,
        "finished" -> game.finishedOrAborted,
        "clock" -> game.hasClock,
        "clockRunning" -> game.isClockRunning,
        "player" -> game.turnColor.name,
        "turns" -> game.turns,
        "startedAtTurn" -> game.startedAtTurn,
        "lastMove" -> game.castleLastMoveTime.lastMoveString),
      "clock" -> game.clock.map(clockJson),
      "player" -> Json.obj(
        "color" -> color.name,
        "version" -> version,
        "spectator" -> true),
      "opponent" -> Json.obj(
        "color" -> opponent.color.name,
        "ai" -> opponent.aiLevel),
      "url" -> Json.obj(
        "socket" -> s"/$gameId/${color.name}/socket",
        "end" -> s"/$gameId/${color.name}/end",
        "table" -> s"/$gameId/${color.name}/table"
      ),
      "pref" -> Json.obj(
        "animationDelay" -> animationDuration(pov, pref),
        "clockTenths" -> pref.clockTenths,
        "clockBar" -> pref.clockBar
      ),
      "possibleMoves" -> possibleMoves(pov),
      "tv" -> tv
    )
  }

  private def getChat(game: Game, forUser: Option[User]) = game.hasChat optionFu {
    chatApi.playerChat find game.id map (_ forUser forUser)
  }

  private def getUsers(game: Game) = UserRepo.pair(
    game.whitePlayer.userId,
    game.blackPlayer.userId)

  private def clockJson(clock: Clock) = Json.obj(
    "initial" -> clock.limit,
    "increment" -> clock.increment,
    "white" -> clock.remainingTime(Color.White),
    "black" -> clock.remainingTime(Color.Black),
    "emerg" -> clock.emergTime
  )

  private def possibleMoves(pov: Pov) = (pov.game playableBy pov.player) option {
    pov.game.toChess.situation.destinations map {
      case (from, dests) => from.key -> dests.mkString
    }
  }

  private def animationFactor(pref: Pref): Float = pref.animation match {
    case 0 => 0
    case 1 => 0.5f
    case 2 => 1
    case 3 => 2
    case _ => 1
  }

  private def animationDuration(pov: Pov, pref: Pref) = round {
    animationFactor(pref) * baseAnimationDuration.toMillis * max(0, min(1.2,
      ((pov.game.estimateTotalTime - 60) / 60) * 0.2
    ))
  }
}
