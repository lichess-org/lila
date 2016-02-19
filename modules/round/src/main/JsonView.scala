package lila.round

import scala.concurrent.duration._
import scala.math

import play.api.libs.json._

import lila.common.Maths.truncateAt
import lila.common.PimpedJson._
import lila.game.JsonView._
import lila.game.{ Pov, Game, PerfPicker, Source, GameRepo, CorrespondenceClock }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import chess.format.Forsyth
import chess.{ Color, Clock }

import actorApi.SocketStatus

final class JsonView(
    chatApi: lila.chat.ChatApi,
    noteApi: NoteApi,
    userJsonView: lila.user.JsonView,
    getSocketStatus: String => Fu[SocketStatus],
    canTakeback: Game => Fu[Boolean],
    baseAnimationDuration: Duration,
    moretimeSeconds: Int) {

  import JsonView._

  private def checkCount(game: Game, color: Color) =
    (game.variant == chess.variant.ThreeCheck) option game.checkCount(color)

  def playerJson(
    pov: Pov,
    pref: Pref,
    apiVersion: Int,
    playerUser: Option[User],
    initialFen: Option[String],
    withBlurs: Boolean): Fu[JsObject] =
    getSocketStatus(pov.game.id) zip
      (pov.opponent.userId ?? UserRepo.byId) zip
      canTakeback(pov.game) zip
      getPlayerChat(pov.game, playerUser) map {
        case (((socket, opponentUser), takebackable), chat) =>
          import pov._
          Json.obj(
            "game" -> gameJson(game, initialFen),
            "clock" -> game.clock.map(clockJson),
            "correspondence" -> game.correspondenceClock,
            "player" -> Json.obj(
              "id" -> playerId,
              "color" -> player.color.name,
              "version" -> socket.version,
              "spectator" -> false,
              "user" -> playerUser.map { userJsonView(_, game.perfType) },
              "rating" -> player.rating,
              "ratingDiff" -> player.ratingDiff,
              "provisional" -> player.provisional.option(true),
              "offeringRematch" -> player.isOfferingRematch.option(true),
              "offeringDraw" -> player.isOfferingDraw.option(true),
              "proposingTakeback" -> player.isProposingTakeback.option(true),
              "onGame" -> (player.isAi || socket.onGame(player.color)),
              "checks" -> checkCount(game, player.color),
              "hold" -> (withBlurs option hold(player)),
              "blurs" -> (withBlurs option blurs(game, player))
            ).noNull,
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView(_, game.perfType) },
              "rating" -> opponent.rating,
              "ratingDiff" -> opponent.ratingDiff,
              "provisional" -> opponent.provisional.option(true),
              "offeringRematch" -> opponent.isOfferingRematch.option(true),
              "offeringDraw" -> opponent.isOfferingDraw.option(true),
              "proposingTakeback" -> opponent.isProposingTakeback.option(true),
              "onGame" -> (opponent.isAi || socket.onGame(opponent.color)),
              "isGone" -> (!opponent.isAi && socket.isGone(opponent.color)),
              "checks" -> checkCount(game, opponent.color),
              "hold" -> (withBlurs option hold(opponent)),
              "blurs" -> (withBlurs option blurs(game, opponent))
            ).noNull,
            "url" -> Json.obj(
              "socket" -> s"/$fullId/socket/v$apiVersion",
              "round" -> s"/$fullId"
            ),
            "pref" -> Json.obj(
              "blindfold" -> pref.isBlindfold,
              "animationDuration" -> animationDuration(pov, pref),
              "highlight" -> (pref.highlight || pref.isBlindfold),
              "destination" -> (pref.destination && !pref.isBlindfold),
              "coords" -> pref.coords,
              "replay" -> pref.replay,
              "autoQueen" -> (pov.game.variant == chess.variant.Antichess).fold(Pref.AutoQueen.NEVER, pref.autoQueen),
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "clockSound" -> pref.clockSound,
              "enablePremove" -> pref.premove,
              "showCaptured" -> pref.captured,
              "submitMove" -> {
                import Pref.SubmitMove._
                pref.submitMove match {
                  case _ if game.hasAi => false
                  case ALWAYS => true
                  case CORRESPONDENCE_UNLIMITED if game.isCorrespondence => true
                  case CORRESPONDENCE_ONLY if game.hasCorrespondenceClock => true
                  case _ => false
                }
              },
              "confirmResign" -> (pref.confirmResign == Pref.ConfirmResign.YES).option(true)),
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
            "possibleDrops" -> possibleDrops(pov),
            "takebackable" -> takebackable,
            "crazyhouse" -> pov.game.crazyData).noNull
      }

  def watcherJson(
    pov: Pov,
    pref: Pref,
    apiVersion: Int,
    user: Option[User],
    tv: Option[OnTv],
    withBlurs: Boolean,
    initialFen: Option[String] = None,
    withMoveTimes: Boolean) =
    getSocketStatus(pov.game.id) zip
      getWatcherChat(pov.game, user) zip
      UserRepo.pair(pov.player.userId, pov.opponent.userId) map {
        case ((socket, chat), (playerUser, opponentUser)) =>
          import pov._
          Json.obj(
            "game" -> {
              gameJson(game, initialFen) ++ Json.obj(
                "moveTimes" -> withMoveTimes.option(game.moveTimes),
                "opening" -> game.opening,
                "importedBy" -> game.pgnImport.flatMap(_.user)).noNull
            },
            "clock" -> game.clock.map(clockJson),
            "correspondence" -> game.correspondenceClock,
            "player" -> Json.obj(
              "color" -> color.name,
              "version" -> socket.version,
              "spectator" -> true,
              "ai" -> player.aiLevel,
              "user" -> playerUser.map { userJsonView(_, game.perfType) },
              "name" -> player.name,
              "rating" -> player.rating,
              "ratingDiff" -> player.ratingDiff,
              "provisional" -> player.provisional.option(true),
              "onGame" -> (player.isAi || socket.onGame(player.color)),
              "checks" -> checkCount(game, player.color),
              "berserk" -> player.berserk.option(true),
              "hold" -> (withBlurs option hold(player)),
              "blurs" -> (withBlurs option blurs(game, player))
            ).noNull,
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView(_, game.perfType) },
              "name" -> opponent.name,
              "rating" -> opponent.rating,
              "ratingDiff" -> opponent.ratingDiff,
              "provisional" -> opponent.provisional.option(true),
              "onGame" -> (opponent.isAi || socket.onGame(opponent.color)),
              "checks" -> checkCount(game, opponent.color),
              "berserk" -> opponent.berserk.option(true),
              "hold" -> (withBlurs option hold(opponent)),
              "blurs" -> (withBlurs option blurs(game, opponent))
            ).noNull,
            "orientation" -> pov.color.name,
            "url" -> Json.obj(
              "socket" -> s"/$gameId/${color.name}/socket",
              "round" -> s"/$gameId/${color.name}"
            ),
            "pref" -> Json.obj(
              "animationDuration" -> animationDuration(pov, pref),
              "highlight" -> pref.highlight,
              "coords" -> pref.coords,
              "replay" -> pref.replay,
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "showCaptured" -> pref.captured
            ),
            "tv" -> tv.map { onTv =>
              Json.obj("channel" -> onTv.channel, "flip" -> onTv.flip)
            },
            "chat" -> chat.map { c =>
              JsArray(c.lines map {
                case lila.chat.UserLine(username, text, _) => Json.obj(
                  "u" -> username,
                  "t" -> text)
              })
            }
          ).noNull
      }

  def userAnalysisJson(pov: Pov, pref: Pref, orientation: chess.Color, owner: Boolean) =
    (pov.game.pgnMoves.nonEmpty ?? GameRepo.initialFen(pov.game)) map { initialFen =>
      import pov._
      val fen = Forsyth >> game.toChess
      Json.obj(
        "game" -> Json.obj(
          "id" -> gameId,
          "variant" -> game.variant,
          "opening" -> game.opening,
          "initialFen" -> {
            if (pov.game.pgnMoves.isEmpty) fen
            else (initialFen | chess.format.Forsyth.initial)
          },
          "fen" -> fen,
          "turns" -> game.turns,
          "player" -> game.turnColor.name,
          "status" -> game.status),
        "player" -> Json.obj(
          "id" -> owner.option(pov.playerId),
          "color" -> color.name
        ),
        "opponent" -> Json.obj(
          "color" -> opponent.color.name,
          "ai" -> opponent.aiLevel
        ),
        "orientation" -> orientation.name,
        "pref" -> Json.obj(
          "animationDuration" -> animationDuration(pov, pref),
          "highlight" -> pref.highlight,
          "destination" -> pref.destination,
          "coords" -> pref.coords
        ),
        "path" -> pov.game.turns,
        "userAnalysis" -> true)
    }

  private def gameJson(game: Game, initialFen: Option[String]) = Json.obj(
    "id" -> game.id,
    "variant" -> game.variant,
    "speed" -> game.speed.key,
    "perf" -> PerfPicker.key(game),
    "rated" -> game.rated,
    "initialFen" -> (initialFen | chess.format.Forsyth.initial),
    "fen" -> (Forsyth >> game.toChess),
    "player" -> game.turnColor.name,
    "winner" -> game.winnerColor.map(_.name),
    "turns" -> game.turns,
    "startedAtTurn" -> game.startedAtTurn,
    "lastMove" -> game.castleLastMoveTime.lastMoveString,
    "threefold" -> game.toChessHistory.threefoldRepetition,
    "check" -> game.check.map(_.key),
    "rematch" -> game.next,
    "source" -> game.source.map(sourceJson),
    "status" -> game.status,
    "boosted" -> game.boosted.option(true),
    "tournamentId" -> game.tournamentId).noNull

  private def blurs(game: Game, player: lila.game.Player) = {
    val percent = game.playerBlurPercent(player.color)
    (percent > 30) option Json.obj(
      "nb" -> player.blurs,
      "percent" -> percent
    )
  }

  private def hold(player: lila.game.Player) = player.holdAlert map { h =>
    Json.obj(
      "ply" -> h.ply,
      "mean" -> h.mean,
      "sd" -> h.sd)
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

  private def sourceJson(source: Source) = source.name

  private def clockJson(clock: Clock): JsObject =
    clockWriter.writes(clock) + ("moretime" -> JsNumber(moretimeSeconds))

  private def possibleMoves(pov: Pov) = (pov.game playableBy pov.player) option {
    pov.game.toChess.situation.destinations map {
      case (from, dests) => from.key -> dests.mkString
    }
  }

  private def possibleDrops(pov: Pov) = (pov.game playableBy pov.player) ?? {
    pov.game.toChess.situation.drops map { drops =>
      JsString(drops.map(_.key).mkString)
    }
  }

  private def animationFactor(pref: Pref): Float = pref.animation match {
    case 0 => 0
    case 1 => 0.5f
    case 2 => 1
    case 3 => 2
    case _ => 1
  }

  private def animationDuration(pov: Pov, pref: Pref) = math.round {
    animationFactor(pref) * baseAnimationDuration.toMillis * pov.game.finished.fold(
      1,
      math.max(0, math.min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
    )
  }
}

object JsonView {

  implicit val variantWriter: OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj(
      "key" -> v.key,
      "name" -> v.name,
      "short" -> v.shortName,
      "title" -> v.title)
  }

  implicit val statusWriter: OWrites[chess.Status] = OWrites { s =>
    Json.obj(
      "id" -> s.id,
      "name" -> s.name)
  }

  implicit val clockWriter: OWrites[Clock] = OWrites { c =>
    Json.obj(
      "running" -> c.isRunning,
      "initial" -> c.limit,
      "increment" -> c.increment,
      "white" -> truncateAt(c.remainingTime(Color.White), 2),
      "black" -> truncateAt(c.remainingTime(Color.Black), 2),
      "emerg" -> c.emergTime)
  }

  implicit val correspondenceWriter: OWrites[CorrespondenceClock] = OWrites { c =>
    Json.obj(
      "daysPerTurn" -> c.daysPerTurn,
      "increment" -> c.increment,
      "white" -> c.whiteTime,
      "black" -> c.blackTime,
      "emerg" -> c.emerg)
  }

  implicit val openingWriter: OWrites[chess.Opening] = OWrites { o =>
    Json.obj(
      "code" -> o.code,
      "name" -> o.name,
      "size" -> o.size
    )
  }
}
