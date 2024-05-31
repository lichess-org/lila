package lila.round

import scala.math

import play.api.libs.json._

import lila.common.ApiVersion
import lila.game.JsonView._
import lila.game.{ Game, Player => GamePlayer, Pov }
import lila.pref.Pref
import lila.user.{ User, UserRepo }

import shogi.Clock

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
    isOfferingRematch: IsOfferingRematch,
    isOfferingResume: IsOfferingResume,
    moretime: MoretimeDuration
)(implicit ec: scala.concurrent.ExecutionContext) {

  import JsonView._

  private val moretimeSeconds = moretime.value.toSeconds.toInt

  private def commonPlayerJson(g: Game, p: GamePlayer, user: Option[User], withFlags: WithFlags): JsObject =
    Json
      .obj(
        "color" -> p.color.name
      )
      .add("user" -> user.map { userJsonView.minimal(_, g.perfType) })
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("offeringRematch" -> g.finishedOrAborted ?? isOfferingRematch(g.id, p.color))
      .add("offeringDraw" -> p.isOfferingDraw)
      .add("proposingTakeback" -> p.isProposingTakeback)
      .add("offeringPause" -> p.isOfferingPause)
      .add("offeringResume" -> g.paused ?? isOfferingResume(g.id, p.color))
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def playerJson(
      pov: Pov,
      pref: Pref,
      apiVersion: ApiVersion,
      playerUser: Option[User],
      withFlags: WithFlags,
      nvui: Boolean
  ): Fu[JsObject] =
    getSocketStatus(pov.game) zip
      (pov.opponent.userId ?? userRepo.byId) zip
      takebacker.isAllowedIn(pov.game) zip
      moretimer.isAllowedIn(pov.game) map { case (((socket, opponentUser), takebackable), moretimeable) =>
        import pov._
        Json
          .obj(
            "game" -> gameJsonView(game),
            "player" -> {
              commonPlayerJson(game, player, playerUser, withFlags) ++ Json.obj(
                "id"      -> playerId,
                "version" -> socket.version.value
              )
            }
              .add("onGame" -> (player.isAi || socket.onGame(player.color)))
              .add(
                "sealedUsi" -> (pov.game.paused && player.color == pov.game.turnColor) ?? pov.game.sealedUsi
                  .map(_.usi)
              ),
            "opponent" -> {
              commonPlayerJson(game, opponent, opponentUser, withFlags) ++ Json.obj(
                "color" -> opponent.color.name,
                "ai"    -> opponent.aiLevel
              )
            }.add("aiCode", opponent.aiCode)
              .add("isGone" -> (!opponent.isAi && socket.isGone(opponent.color)))
              .add("onGame" -> (opponent.isAi || socket.onGame(opponent.color))),
            "url" -> Json.obj(
              "socket" -> s"/play/$fullId/v$apiVersion",
              "round"  -> s"/$fullId"
            ),
            "pref" -> Json
              .obj(
                "animationDuration" -> animationMillis(pov, pref),
                "coords"            -> pref.coords,
                "resizeHandle"      -> pref.resizeHandle,
                "replay"            -> pref.replay,
                "clockTenths"       -> pref.clockTenths,
                "clockCountdown"    -> pref.clockCountdown,
                "moveEvent"         -> pref.moveEvent
              )
              .add("clockSound" -> pref.clockSound)
              .add("confirmResign" -> (!nvui && pref.confirmResign == Pref.ConfirmResign.YES))
              .add("keyboardMove" -> (!nvui && pref.keyboardMove == Pref.KeyboardMove.YES))
              .add("blindfold" -> pref.isBlindfold)
              .add("highlightLastDests" -> pref.highlightLastDests)
              .add("highlightCheck" -> pref.highlightCheck)
              .add("squareOverlay" -> pref.squareOverlay)
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("dropDestination" -> (pref.dropDestination && !pref.isBlindfold))
              .add("enablePremove" -> pref.premove)
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
      .add("aiCode" -> p.aiCode)
      .add("rating" -> p.rating)
      .add("ratingDiff" -> p.ratingDiff)
      .add("provisional" -> p.provisional)
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs ?? blurs(g, p)))

  def watcherJson(
      pov: Pov,
      pref: Pref,
      apiVersion: ApiVersion,
      me: Option[User],
      tv: Option[OnTv],
      withFlags: WithFlags
  ) =
    getSocketStatus(pov.game) zip
      userRepo.pair(pov.player.userId, pov.opponent.userId) map { case (socket, (playerUser, opponentUser)) =>
        import pov._
        Json
          .obj(
            "game" -> gameJsonView(game)
              .add("moveCentis" -> (withFlags.movetimes ?? game.moveTimes.map(_.map(_.centis))))
              .add("division" -> withFlags.division.option(divider(game)))
              .add("importedBy" -> game.notationImport.flatMap(_.user)),
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
                "animationDuration" -> animationMillis(pov, pref),
                "coords"            -> pref.coords,
                "resizeHandle"      -> pref.resizeHandle,
                "replay"            -> pref.replay,
                "clockTenths"       -> pref.clockTenths,
                "clockCountdown"    -> pref.clockCountdown,
                "moveEvent"         -> pref.moveEvent
              )
              .add("highlightLastDests" -> pref.highlightLastDests)
              .add("highlightCheck" -> pref.highlightCheck)
              .add("squareOverlay" -> pref.squareOverlay)
              .add("destination" -> (pref.destination && !pref.isBlindfold))
              .add("dropDestination" -> (pref.dropDestination && !pref.isBlindfold)),
            "evalPut" -> JsBoolean(me.??(evalCache.shouldPut))
          )
          .add("evalPut" -> me.??(evalCache.shouldPut))
          .add("tv" -> tv.collect { case OnLishogiTv(channel, flip) =>
            Json.obj("channel" -> channel, "flip" -> flip)
          })
          .add("userTv" -> tv.collect { case OnUserTv(userId) =>
            Json.obj("id" -> userId)
          })

      }

  def userAnalysisJson(
      pov: Pov,
      pref: Pref,
      orientation: shogi.Color,
      owner: Boolean,
      me: Option[User],
      division: Option[shogi.Division] = none
  ) = {
    import pov._
    Json
      .obj(
        "game" -> Json
          .obj(
            "id"            -> gameId,
            "variant"       -> game.variant,
            "initialSfen"   -> (game.initialSfen | game.variant.initialSfen),
            "sfen"          -> game.shogi.toSfen,
            "plies"         -> game.plies,
            "player"        -> game.turnColor.name,
            "status"        -> game.status,
            "source"        -> game.source,
            "startedAtStep" -> game.shogi.startedAtStep,
            "startedAtPly"  -> game.shogi.startedAtPly
          )
          .add("division", division)
          .add("winner", game.winner.map(_.color.name)),
        "player" -> Json.obj(
          "id"    -> owner.option(pov.playerId),
          "color" -> color.name,
          "name"  -> player.name
        ),
        "opponent" -> Json
          .obj(
            "color" -> opponent.color.name,
            "ai"    -> opponent.aiLevel,
            "name"  -> opponent.name
          )
          .add("aiCode", opponent.aiCode),
        "orientation" -> orientation.name,
        "pref" -> Json
          .obj(
            "animationDuration" -> animationMillis(pov, pref),
            "coords"            -> pref.coords,
            "moveEvent"         -> pref.moveEvent,
            "resizeHandle"      -> pref.resizeHandle
          )
          .add("highlightLastDests" -> pref.highlightLastDests)
          .add("highlightCheck" -> pref.highlightCheck)
          .add("squareOverlay" -> pref.squareOverlay)
          .add("destination" -> (pref.destination && !pref.isBlindfold))
          .add("dropDestination" -> (pref.dropDestination && !pref.isBlindfold)),
        "path"         -> pov.game.plies,
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

  private def animationMillis(pov: Pov, pref: Pref) =
    pref.animationMillis * {
      if (pov.game.finished) 1
      else math.max(0, math.min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
    }
}

object JsonView {

  case class WithFlags(
      movetimes: Boolean = false,
      division: Boolean = false,
      clocks: Boolean = false,
      blurs: Boolean = false
  )
}
