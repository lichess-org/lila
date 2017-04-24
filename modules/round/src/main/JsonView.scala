package lila.round

import scala.concurrent.duration._
import scala.math

import org.apache.commons.lang3.StringEscapeUtils.escapeHtml4
import play.api.libs.json._

import lila.common.PimpedJson._
import lila.common.ApiVersion
import lila.game.JsonView._
import lila.game.{ Pov, Game, PerfPicker, Source, GameRepo, CorrespondenceClock }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import chess.format.Forsyth
import chess.{ Centis, Color, Clock }

import actorApi.SocketStatus

final class JsonView(
    noteApi: NoteApi,
    userJsonView: lila.user.JsonView,
    getSocketStatus: String => Fu[SocketStatus],
    canTakeback: Game => Fu[Boolean],
    divider: lila.game.Divider,
    evalCache: lila.evalCache.EvalCacheApi,
    baseAnimationDuration: Duration,
    moretimeSeconds: Int
) {

  import JsonView._

  private def checkCount(game: Game, color: Color) =
    (game.variant == chess.variant.ThreeCheck) option game.checkCount(color)

  def playerJson(
    pov: Pov,
    pref: Pref,
    apiVersion: ApiVersion,
    playerUser: Option[User],
    initialFen: Option[String],
    withFlags: WithFlags
  ): Fu[JsObject] =
    getSocketStatus(pov.game.id) zip
      (pov.opponent.userId ?? UserRepo.byId) zip
      canTakeback(pov.game) map {
        case ((socket, opponentUser), takebackable) =>
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
              "user" -> playerUser.map { userJsonView.minimal(_, game.perfType) },
              "rating" -> player.rating,
              "ratingDiff" -> player.ratingDiff,
              "provisional" -> player.provisional.option(true),
              "offeringRematch" -> player.isOfferingRematch.option(true),
              "offeringDraw" -> player.isOfferingDraw.option(true),
              "proposingTakeback" -> player.isProposingTakeback.option(true),
              "onGame" -> (player.isAi || socket.onGame(player.color)),
              "checks" -> checkCount(game, player.color),
              "berserk" -> player.berserk.option(true),
              "hold" -> (withFlags.blurs option hold(player)),
              "blurs" -> (withFlags.blurs option blurs(game, player))
            ).noNull,
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView.minimal(_, game.perfType) },
              "rating" -> opponent.rating,
              "ratingDiff" -> opponent.ratingDiff,
              "provisional" -> opponent.provisional.option(true),
              "offeringRematch" -> opponent.isOfferingRematch.option(true),
              "offeringDraw" -> opponent.isOfferingDraw.option(true),
              "proposingTakeback" -> opponent.isProposingTakeback.option(true),
              "onGame" -> (opponent.isAi || socket.onGame(opponent.color)),
              "isGone" -> (!opponent.isAi && socket.isGone(opponent.color)),
              "checks" -> checkCount(game, opponent.color),
              "berserk" -> opponent.berserk.option(true),
              "hold" -> (withFlags.blurs option hold(opponent)),
              "blurs" -> (withFlags.blurs option blurs(game, opponent))
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
              "confirmResign" -> (pref.confirmResign == Pref.ConfirmResign.YES).option(true),
              "moveEvent" -> pref.moveEvent,
              "keyboardMove" -> (pref.keyboardMove == Pref.KeyboardMove.YES).option(true),
              "rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES),
              "is3d" -> pref.is3d
            ),
            "possibleMoves" -> possibleMoves(pov),
            "possibleDrops" -> possibleDrops(pov),
            "takebackable" -> takebackable,
            "crazyhouse" -> pov.game.crazyData
          ).noNull
      }

  def watcherJson(
    pov: Pov,
    pref: Pref,
    apiVersion: ApiVersion,
    me: Option[User],
    tv: Option[OnTv],
    initialFen: Option[String] = None,
    withFlags: WithFlags
  ) =
    getSocketStatus(pov.game.id) zip
      UserRepo.pair(pov.player.userId, pov.opponent.userId) map {
        case (socket, (playerUser, opponentUser)) =>
          import pov._
          Json.obj(
            "game" -> {
              gameJson(game, initialFen) ++ Json.obj(
                "moveCentis" -> withFlags.movetimes ?? game.moveTimes.map(_.map(_.centis)),
                "division" -> withFlags.division.option(divider(game, initialFen)),
                "opening" -> game.opening,
                "importedBy" -> game.pgnImport.flatMap(_.user)
              ).noNull
            },
            "clock" -> game.clock.map(clockJson),
            "correspondence" -> game.correspondenceClock,
            "player" -> Json.obj(
              "color" -> color.name,
              "version" -> socket.version,
              "spectator" -> true,
              "ai" -> player.aiLevel,
              "user" -> playerUser.map { userJsonView.minimal(_, game.perfType) },
              "name" -> player.name.map(escapeHtml4),
              "rating" -> player.rating,
              "ratingDiff" -> player.ratingDiff,
              "provisional" -> player.provisional.option(true),
              "onGame" -> (player.isAi || socket.onGame(player.color)),
              "checks" -> checkCount(game, player.color),
              "berserk" -> player.berserk.option(true),
              "hold" -> (withFlags.blurs option hold(player)),
              "blurs" -> (withFlags.blurs option blurs(game, player))
            ).noNull,
            "opponent" -> Json.obj(
              "color" -> opponent.color.name,
              "ai" -> opponent.aiLevel,
              "user" -> opponentUser.map { userJsonView.minimal(_, game.perfType) },
              "name" -> opponent.name.map(escapeHtml4),
              "rating" -> opponent.rating,
              "ratingDiff" -> opponent.ratingDiff,
              "provisional" -> opponent.provisional.option(true),
              "onGame" -> (opponent.isAi || socket.onGame(opponent.color)),
              "checks" -> checkCount(game, opponent.color),
              "berserk" -> opponent.berserk.option(true),
              "hold" -> (withFlags.blurs option hold(opponent)),
              "blurs" -> (withFlags.blurs option blurs(game, opponent))
            ).noNull,
            "orientation" -> pov.color.name,
            "url" -> Json.obj(
              "socket" -> s"/$gameId/${color.name}/socket",
              "round" -> s"/$gameId/${color.name}"
            ),
            "pref" -> Json.obj(
              "animationDuration" -> animationDuration(pov, pref),
              "destination" -> pref.destination,
              "highlight" -> pref.highlight,
              "coords" -> pref.coords,
              "replay" -> pref.replay,
              "clockTenths" -> pref.clockTenths,
              "clockBar" -> pref.clockBar,
              "showCaptured" -> pref.captured,
              "is3d" -> pref.is3d
            ),
            "evalPut" -> JsBoolean(me.??(evalCache.shouldPut))
          ).add(
              "tv" -> tv.collect {
                case OnLichessTv(channel, flip) => Json.obj("channel" -> channel, "flip" -> flip)
              }
            ).add(
                "userTv" -> tv.collect {
                  case OnUserTv(userId) => Json.obj("id" -> userId)
                }
              )

      }

  private implicit val userLineWrites = OWrites[lila.chat.UserLine] { l =>
    val j = Json.obj("u" -> l.username, "t" -> l.text)
    if (l.deleted) j + ("d" -> JsBoolean(true)) else j
  }

  def userAnalysisJson(
    pov: Pov,
    pref: Pref,
    orientation: chess.Color,
    owner: Boolean,
    me: Option[User]
  ) =
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
          "status" -> game.status
        ),
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
          "blindfold" -> pref.isBlindfold,
          "animationDuration" -> animationDuration(pov, pref),
          "highlight" -> pref.highlight,
          "destination" -> pref.destination,
          "coords" -> pref.coords,
          "rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES),
          "is3d" -> pref.is3d
        ),
        "path" -> pov.game.turns,
        "userAnalysis" -> true,
        "evalPut" -> JsBoolean(me.??(evalCache.shouldPut))
      )
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
    "threefold" -> game.toChessHistory.threefoldRepetition.option(true),
    "check" -> game.check.map(_.key),
    "rematch" -> game.next,
    "source" -> game.source.map(sourceJson),
    "status" -> game.status,
    "boosted" -> game.boosted.option(true),
    "tournamentId" -> game.tournamentId,
    "createdAt" -> game.createdAt
  ).noNull

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
      "sd" -> h.sd
    )
  }

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

  case class WithFlags(
    opening: Boolean = false,
    movetimes: Boolean = false,
    division: Boolean = false,
    clocks: Boolean = false,
    blurs: Boolean = false
  )

  implicit val variantWriter: OWrites[chess.variant.Variant] = OWrites { v =>
    Json.obj(
      "key" -> v.key,
      "name" -> v.name,
      "short" -> v.shortName
    )
  }

  implicit val clockWriter: OWrites[Clock] = OWrites { c =>
    import lila.common.Maths.truncateAt
    Json.obj(
      "running" -> c.isRunning,
      "initial" -> c.limitSeconds,
      "increment" -> c.incrementSeconds,
      "white" -> c.remainingTime(Color.White).toSeconds,
      "black" -> c.remainingTime(Color.Black).toSeconds,
      "emerg" -> c.emergTime
    )
  }

  implicit val correspondenceWriter: OWrites[CorrespondenceClock] = OWrites { c =>
    Json.obj(
      "daysPerTurn" -> c.daysPerTurn,
      "increment" -> c.increment,
      "white" -> c.whiteTime,
      "black" -> c.blackTime,
      "emerg" -> c.emerg
    )
  }

  implicit val openingWriter: OWrites[chess.opening.FullOpening.AtPly] = OWrites { o =>
    Json.obj(
      "eco" -> o.opening.eco,
      "name" -> o.opening.name,
      "ply" -> o.ply
    )
  }

  implicit val divisionWriter: OWrites[chess.Division] = OWrites { o =>
    Json.obj(
      "middle" -> o.middle,
      "end" -> o.end
    )
  }
}
