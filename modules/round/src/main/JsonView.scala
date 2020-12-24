package lila.round

import scala.math

import play.api.libs.json._

import lila.common.ApiVersion
import lila.game.JsonView._
import lila.game.{ Pov, Game, Player => GamePlayer }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import chess.format.{ FEN, Forsyth }
import chess.{ Clock, Color }

import actorApi.SocketStatus

final class JsonView(
    userRepo: UserRepo,
    userJsonView: lila.user.JsonView,
    gameJsonView: lila.game.JsonView,
    getSocketStatus: Game => Fu[SocketStatus],
    takebacker: Takebacker,
    moretimer: Moretimer,
    divider: lila.game.Divider,
    evalCache: lila.evalCache.EvalCacheApi,
    isOfferingRematch: Pov => Boolean,
    animation: AnimationDuration,
    moretime: MoretimeDuration
)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonView._

  private val moretimeSeconds = moretime.value.toSeconds.toInt

  private def checkCount(game: Game, color: Color) =
    (game.variant == chess.variant.ThreeCheck) option game.history.checkCount(color)

  private def commonPlayerJson(g: Game, p: GamePlayer, user: Option[User], withFlags: WithFlags): JsObject =
    Json
      .obj(
        "color" -> p.color.name
      )
      .add("user" -> user.map { userJsonView.minimal(_, g.perfType) })
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("offeringRematch" -> isOfferingRematch(Pov(g, p)))
      .add("offeringDraw" -> p.isOfferingDraw)
      .add("proposingTakeback" -> p.isProposingTakeback)
      .add("checks" -> checkCount(g, p.color))
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def playerJson(
      pov: Pov,
      pref: Pref,
      apiVersion: ApiVersion,
      playerUser: Option[User],
      initialFen: Option[FEN],
      withFlags: WithFlags,
      nvui: Boolean
  ): Fu[JsObject] =
    getSocketStatus(pov.game) zip
      (pov.opponent.userId ?? userRepo.byId) zip
      takebacker.isAllowedIn(pov.game) zip
      moretimer.isAllowedIn(pov.game) map { case socket ~ opponentUser ~ takebackable ~ moretimeable =>
        import pov._
        Json
          .obj(
            "game" -> gameJsonView(game, initialFen),
            "player" -> {
              commonPlayerJson(game, player, playerUser, withFlags) ++ Json.obj(
                "id"      -> playerId,
                "version" -> socket.version.value
              )
            }.add("onGame" -> (player.isAi || socket.onGame(player.color))),
            "opponent" -> {
              commonPlayerJson(game, opponent, opponentUser, withFlags) ++ Json.obj(
                "color" -> opponent.color.name,
                "ai"    -> opponent.aiLevel
              )
            }.add("isGone" -> (!opponent.isAi && socket.isGone(opponent.color)))
              .add("onGame" -> (opponent.isAi || socket.onGame(opponent.color))),
            "url" -> Json.obj(
              "socket" -> s"/play/$fullId/v$apiVersion",
              "round"  -> s"/$fullId"
            ),
            "pref" -> Json
              .obj(
                "animationDuration" -> animationDuration(pov, pref),
                "coords"            -> pref.coords,
                "resizeHandle"      -> pref.resizeHandle,
                "replay"            -> pref.replay,
                "autoQueen" -> (if (pov.game.variant == chess.variant.Antichess) Pref.AutoQueen.NEVER
                                else pref.autoQueen),
                "clockTenths" -> pref.clockTenths,
                "moveEvent"   -> pref.moveEvent
              )
              .add("is3d" -> pref.is3d)
              .add("clockBar" -> pref.clockBar)
              .add("clockSound" -> pref.clockSound)
              .add("confirmResign" -> (!nvui && pref.confirmResign == Pref.ConfirmResign.YES))
              .add("keyboardMove" -> (!nvui && pref.keyboardMove == Pref.KeyboardMove.YES))
              .add("rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES))
              .add("blindfold" -> pref.isBlindfold)
              .add("highlight" -> pref.highlight)
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("enablePremove" -> pref.premove)
              .add("showCaptured" -> pref.captured)
              .add("submitMove" -> {
                import Pref.SubmitMove._
                pref.submitMove match {
                  case _ if game.hasAi || nvui                            => false
                  case ALWAYS                                             => true
                  case CORRESPONDENCE_UNLIMITED if game.isCorrespondence  => true
                  case CORRESPONDENCE_ONLY if game.hasCorrespondenceClock => true
                  case _                                                  => false
                }
              })
          )
          .add("clock" -> game.clock.map(clockJson))
          .add("correspondence" -> game.correspondenceClock)
          .add("takebackable" -> takebackable)
          .add("moretimeable" -> moretimeable)
          .add("crazyhouse" -> pov.game.board.crazyData)
          .add("possibleMoves" -> possibleMoves(pov, apiVersion))
          .add("possibleOppositeMoves" -> possibleOppositeMoves(pov, apiVersion))
          .add("possibleDrops" -> possibleDrops(pov))
          .add("expiration" -> game.expirable.option {
            Json.obj(
              "idleMillis"   -> (nowMillis - game.movedAt.getMillis),
              "millisToMove" -> game.timeForFirstMove.millis
            )
          })
      }

  private def commonWatcherJson(g: Game, p: GamePlayer, user: Option[User], withFlags: WithFlags): JsObject =
    Json
      .obj(
        "color" -> p.color.name,
        "name"  -> p.name
      )
      .add("user" -> user.map { userJsonView.minimal(_, g.perfType) })
      .add("ai" -> p.aiLevel)
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("checks" -> checkCount(g, p.color))
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def watcherJson(
      pov: Pov,
      pref: Pref,
      apiVersion: ApiVersion,
      me: Option[User],
      tv: Option[OnTv],
      initialFen: Option[FEN] = None,
      withFlags: WithFlags
  ) =
    getSocketStatus(pov.game) zip
      userRepo.pair(pov.player.userId, pov.opponent.userId) map { case (socket, (playerUser, opponentUser)) =>
        import pov._
        Json
          .obj(
            "game" -> gameJsonView(game, initialFen)
              .add("moveCentis" -> (withFlags.movetimes ?? game.moveTimes.map(_.map(_.centis))))
              .add("division" -> withFlags.division.option(divider(game, initialFen)))
              .add("opening" -> game.opening)
              .add("importedBy" -> game.pgnImport.flatMap(_.user)),
            "clock"          -> game.clock.map(clockJson),
            "correspondence" -> game.correspondenceClock,
            "player" -> {
              commonWatcherJson(game, player, playerUser, withFlags) ++ Json.obj(
                "version"   -> socket.version.value,
                "spectator" -> true
              )
            }.add("onGame" -> (player.isAi || socket.onGame(player.color))),
            "opponent" -> commonWatcherJson(game, opponent, opponentUser, withFlags).add(
              "onGame" -> (opponent.isAi || socket.onGame(opponent.color))
            ),
            "orientation" -> pov.color.name,
            "url" -> Json.obj(
              "socket" -> s"/watch/$gameId/${color.name}/v$apiVersion",
              "round"  -> s"/$gameId/${color.name}"
            ),
            "pref" -> Json
              .obj(
                "animationDuration" -> animationDuration(pov, pref),
                "coords"            -> pref.coords,
                "resizeHandle"      -> pref.resizeHandle,
                "replay"            -> pref.replay,
                "clockTenths"       -> pref.clockTenths
              )
              .add("is3d" -> pref.is3d)
              .add("clockBar" -> pref.clockBar)
              .add("highlight" -> pref.highlight)
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES))
              .add("showCaptured" -> pref.captured),
            "evalPut" -> JsBoolean(me.??(evalCache.shouldPut))
          )
          .add("evalPut" -> me.??(evalCache.shouldPut))
          .add("tv" -> tv.collect { case OnLichessTv(channel, flip) =>
            Json.obj("channel" -> channel, "flip" -> flip)
          })
          .add("userTv" -> tv.collect { case OnUserTv(userId) =>
            Json.obj("id" -> userId)
          })

      }

  def userAnalysisJson(
      pov: Pov,
      pref: Pref,
      initialFen: Option[FEN],
      orientation: chess.Color,
      owner: Boolean,
      me: Option[User],
      division: Option[chess.Division] = none
  ) = {
    import pov._
    val fen = Forsyth >> game.chess
    Json
      .obj(
        "game" -> Json
          .obj(
            "id"         -> gameId,
            "variant"    -> game.variant,
            "opening"    -> game.opening,
            "initialFen" -> (initialFen | chess.format.Forsyth.initial),
            "fen"        -> fen,
            "turns"      -> game.turns,
            "player"     -> game.turnColor.name,
            "status"     -> game.status
          )
          .add("division", division)
          .add("winner", game.winner.map(_.color.name)),
        "player" -> Json.obj(
          "id"    -> owner.option(pov.playerId),
          "color" -> color.name
        ),
        "opponent" -> Json.obj(
          "color" -> opponent.color.name,
          "ai"    -> opponent.aiLevel
        ),
        "orientation" -> orientation.name,
        "pref" -> Json
          .obj(
            "animationDuration" -> animationDuration(pov, pref),
            "coords"            -> pref.coords,
            "moveEvent"         -> pref.moveEvent,
            "resizeHandle"      -> pref.resizeHandle
          )
          .add("rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES))
          .add("is3d" -> pref.is3d)
          .add("highlight" -> pref.highlight)
          .add("destination" -> (pref.destination && !pref.isBlindfold)),
        "path"         -> pov.game.turns,
        "userAnalysis" -> true
      )
      .add("evalPut" -> me.??(evalCache.shouldPut))
  }

  private def blurs(game: Game, player: lila.game.Player) =
    player.blurs.nonEmpty option {
      blursWriter.writes(player.blurs) +
        ("percent" -> JsNumber(game.playerBlurPercent(player.color)))
    }

  private def clockJson(clock: Clock): JsObject =
    clockWriter.writes(clock) + ("moretime" -> JsNumber(moretimeSeconds))

  private def possibleMoves(pov: Pov, apiVersion: ApiVersion): Option[JsValue] =
    (pov.game playableBy pov.player) option
      lila.game.Event.PossibleMoves.json(pov.game.situation.destinations, apiVersion)

  private def possibleOppositeMoves(pov: Pov, apiVersion: ApiVersion): Option[JsValue] =
    (pov.game playableBy pov.player) option
      lila.game.Event.PossibleMoves.json((!pov).game.situation.destinations, apiVersion)

  private def possibleDrops(pov: Pov): Option[JsValue] =
    (pov.game playableBy pov.player) ?? {
      pov.game.situation.drops map { drops =>
        JsString(drops.map(_.key).mkString)
      }
    }

  private def animationFactor(pref: Pref): Float =
    pref.animation match {
      case 0 => 0
      case 1 => 0.5f
      case 2 => 1
      case 3 => 2
      case _ => 1
    }

  private def animationDuration(pov: Pov, pref: Pref) =
    math.round {
      animationFactor(pref) * animation.value.toMillis * {
        if (pov.game.finished) 1
        else math.max(0, math.min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
      }
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
}
