package lila.round

import scala.concurrent.duration._
import scala.math.{ min, max, round }

import play.api.libs.json._

import lila.common.PimpedJson._
import lila.game.{ Pov, Game, PerfPicker }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import chess.format.Forsyth
import chess.{ Color, Clock, Variant }

final class JsonView(
    chatApi: lila.chat.ChatApi,
    userJsonView: lila.user.JsonView,
    getVersion: String => Fu[Int],
    canTakeback: Game => Fu[Boolean],
    baseAnimationDuration: Duration,
    moretimeSeconds: Int) {

  private def variantJson(v: Variant) = Json.obj(
    "key" -> v.key,
    "name" -> v.name,
    "short" -> v.shortName,
    "title" -> v.title)

  def playerJson(
    pov: Pov,
    pref: Pref,
    apiVersion: Int,
    playerUser: Option[User]): Fu[JsObject] =
    getVersion(pov.game.id) zip
      (pov.opponent.userId ?? UserRepo.byId) zip
      canTakeback(pov.game) zip
      getPlayerChat(pov.game, playerUser) map {
        case (((version, opponentUser), takebackable), chat) =>
          import pov._
          Json.obj(
            "game" -> Json.obj(
              "id" -> gameId,
              "variant" -> variantJson(game.variant),
              "speed" -> game.speed.key,
              "perf" -> PerfPicker.key(game),
              "rated" -> game.rated,
              "fen" -> (Forsyth >> game.toChess),
              "moves" -> game.pgnMoves.mkString(" "),
              "player" -> game.turnColor.name,
              "winner" -> game.winnerColor.map(_.name),
              "turns" -> game.turns,
              "startedAtTurn" -> game.startedAtTurn,
              "lastMove" -> game.castleLastMoveTime.lastMoveString,
              "threefold" -> game.toChessHistory.threefoldRepetition,
              "check" -> game.check.map(_.key),
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
              "coords" -> pref.coords,
              "autoQueen" -> pref.autoQueen,
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "enablePremove" -> pref.premove,
              "showCaptured" -> pref.captured
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

  def watcherJson(
    pov: Pov,
    pref: Pref,
    apiVersion: Int,
    user: Option[User],
    tv: Boolean) =
    getVersion(pov.game.id) zip
      getWatcherChat(pov.game, user) zip
      UserRepo.pair(pov.player.userId, pov.opponent.userId) map {
        case ((version, chat), (playerUser, opponentUser)) =>
          import pov._
          Json.obj(
            "game" -> Json.obj(
              "id" -> gameId,
              "variant" -> variantJson(game.variant),
              "speed" -> game.speed.key,
              "perf" -> PerfPicker.key(game),
              "rated" -> game.rated,
              "fen" -> (Forsyth >> game.toChess),
              "player" -> game.turnColor.name,
              "turns" -> game.turns,
              "startedAtTurn" -> game.startedAtTurn,
              "lastMove" -> game.castleLastMoveTime.lastMoveString,
              "check" -> game.check.map(_.key),
              "rematch" -> game.next,
              "status" -> Json.obj(
                "id" -> pov.game.status.id,
                "name" -> pov.game.status.name)),
            "clock" -> game.clock.map(clockJson),
            "player" -> Json.obj(
              "color" -> color.name,
              "version" -> version,
              "spectator" -> true,
              "ai" -> player.aiLevel,
              "user" -> playerUser.map { userJsonView(_, true) }),
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView(_, true) }),
            "url" -> Json.obj(
              "socket" -> s"/$gameId/${color.name}/socket",
              "round" -> s"/$gameId/${color.name}"
            ),
            "pref" -> Json.obj(
              "animationDelay" -> animationDuration(pov, pref),
              "highlight" -> pref.highlight,
              "coords" -> pref.coords,
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "showCaptured" -> pref.captured
            ),
            "tv" -> tv,
            "chat" -> chat.map { c =>
              JsArray(c.lines map {
                case lila.chat.UserLine(username, text, _) => Json.obj(
                  "u" -> username,
                  "t" -> text)
              })
            }
          )
      }

  private def getPlayerChat(game: Game, forUser: Option[User]): Fu[Option[lila.chat.MixedChat]] =
    game.hasChat optionFu {
      chatApi.playerChat find game.id map (_ forUser forUser)
    }

  private def getWatcherChat(game: Game, forUser: Option[User]): Fu[Option[lila.chat.UserChat]] =
    forUser ?? { user =>
      chatApi.userChat find s"${game.id}/w" map (_ forUser user.some) map (_.some)
    }

  private def getUsers(game: Game) = UserRepo.pair(
    game.whitePlayer.userId,
    game.blackPlayer.userId)

  private def clockJson(clock: Clock) = Json.obj(
    "running" -> clock.isRunning,
    "initial" -> clock.limit,
    "increment" -> clock.increment,
    "white" -> clock.remainingTime(Color.White),
    "black" -> clock.remainingTime(Color.Black),
    "emerg" -> clock.emergTime,
    "moretime" -> moretimeSeconds
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
