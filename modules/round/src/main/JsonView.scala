package lila.round

import actorApi.SocketStatus
import chess.format.Fen
import chess.{ Speed, ByColor, Clock, Color }
import play.api.libs.json.*
import scala.math

import lila.common.{ Preload, ApiVersion, LightUser }
import lila.common.Json.given
import lila.game.JsonView.given
import lila.game.{ Game, Player as GamePlayer, Pov }
import lila.pref.Pref
import lila.user.{ User, GameUser, GameUsers }
import lila.rating.{ PerfType, Perf }

final class JsonView(
    lightUserGet: LightUser.Getter,
    userJsonView: lila.user.JsonView,
    gameJsonView: lila.game.JsonView,
    getSocketStatus: Game => Fu[SocketStatus],
    takebacker: Takebacker,
    moretimer: Moretimer,
    divider: lila.game.Divider,
    isOfferingRematch: IsOfferingRematch
)(using Executor):

  import JsonView.*

  private def checkCount(game: Game, color: Color) =
    (game.variant == chess.variant.ThreeCheck) option game.history.checkCount(color)

  private def commonPlayerJson(
      g: Game,
      p: GamePlayer,
      user: GameUser,
      withFlags: WithFlags
  ): JsObject =
    Json
      .obj("color" -> p.color.name)
      .add("user" -> user.match
        case Some(User.WithPerf(user, perf)) =>
          val p = withFlags.rating.option(Perf.Typed(perf, g.perfType))
          userJsonView.roundPlayer(user, p).some
        case _ if p.hasUser => userJsonView.ghost.some
        case _              => none
      )
      .add("rating" -> p.rating.ifTrue(withFlags.rating))
      .add("ratingDiff" -> p.ratingDiff.ifTrue(withFlags.rating))
      .add("provisional" -> (p.provisional.yes && withFlags.rating))
      .add("offeringRematch" -> isOfferingRematch(Pov(g, p)))
      .add("offeringDraw" -> p.isOfferingDraw)
      .add("proposingTakeback" -> p.isProposingTakeback)
      .add("checks" -> checkCount(g, p.color))
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs so blurs(g, p)))

  def playerJson(
      pov: Pov,
      pref: Option[Pref],
      users: GameUsers,
      initialFen: Option[Fen.Epd],
      flags: WithFlags
  ): Fu[JsObject] =
    getSocketStatus(pov.game) zip
      takebacker.isAllowedIn(pov.game) zip
      moretimer.isAllowedIn(pov.game) map { case ((socket, takebackable), moretimeable) =>
        import pov.*
        Json
          .obj(
            "game" -> gameJsonView.baseWithChessDenorm(game, initialFen),
            "player" -> {
              commonPlayerJson(game, player, users(pov.color), flags) ++ Json
                .obj(
                  "id"      -> playerId,
                  "version" -> socket.version
                )
                .add("onGame" -> (player.isAi || socket.onGame(player.color)))
                .add("lastDrawOfferAtPly" -> game.metadata.drawOffers.lastBy(player.color))
            },
            "opponent" -> {
              commonPlayerJson(game, opponent, users(pov.opponent.color), flags) ++ Json
                .obj("color" -> opponent.color.name)
                .add("ai" -> opponent.aiLevel)
                .add("isGone" -> (!opponent.isAi && socket.isGone(opponent.color)))
                .add("onGame" -> (opponent.isAi || socket.onGame(opponent.color)))
            },
            "url" -> flags.lichobileCompat.option:
              Json.obj(
                "socket" -> s"/play/$fullId/v${ApiVersion.lichobile}",
                "round"  -> s"/$fullId"
              )
          )
          .add(
            "pref" -> pref.map: pref =>
              Json
                .obj(
                  "animationDuration" -> animationMillis(pov, pref),
                  "coords"            -> pref.coords,
                  "resizeHandle"      -> pref.resizeHandle,
                  "replay"            -> pref.replay,
                  "autoQueen" -> (if pov.game.variant == chess.variant.Antichess then Pref.AutoQueen.NEVER
                                  else pref.autoQueen),
                  "clockTenths" -> pref.clockTenths,
                  "moveEvent"   -> pref.moveEvent
                  // "ratings"     -> pref.showRatings
                )
                .add("is3d" -> pref.is3d)
                .add("clockBar" -> pref.clockBar)
                .add("clockSound" -> pref.clockSound)
                .add("confirmResign" -> (!flags.nvui && pref.confirmResign == Pref.ConfirmResign.YES))
                .add("keyboardMove" -> (!flags.nvui && pref.hasKeyboardMove))
                .add("voiceMove" -> (!flags.nvui && pref.hasVoice))
                .add("rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES))
                .add("blindfold" -> pref.isBlindfold)
                .add("highlight" -> pref.highlight)
                .add("destination" -> (pref.destination && !pref.isBlindfold))
                .add("enablePremove" -> pref.premove)
                .add("showCaptured" -> pref.captured)
                .add("submitMove" -> {
                  import Pref.SubmitMove.*
                  pref.submitMove match
                    case _ if game.hasAi || flags.nvui                                 => false
                    case n if (n & UNLIMITED) != 0 && game.isUnlimited                 => true
                    case n if (n & CORRESPONDENCE) != 0 && game.hasCorrespondenceClock => true
                    case n if (n & CLASSICAL) != 0 && game.isSpeed(Speed.Classical)    => true
                    case n if (n & RAPID) != 0 && game.isSpeed(Speed.Rapid)            => true
                    case n if (n & BLITZ) != 0 && game.isSpeed(Speed.Blitz)            => true
                    case _                                                             => false
                })
          )
          .add("clock" -> game.clock.map(clockJson))
          .add("correspondence" -> game.correspondenceClock)
          .add("takebackable" -> takebackable)
          .add("moretimeable" -> moretimeable)
          .add("crazyhouse" -> pov.game.board.crazyData)
          .add("possibleMoves" -> possibleMoves(pov))
          .add("possibleDrops" -> possibleDrops(pov))
          .add("expiration" -> game.expirable.option:
            Json.obj(
              "idleMillis"   -> (nowMillis - game.movedAt.toMillis),
              "millisToMove" -> game.timeForFirstMove.millis
            )
          )
      }

  private def commonWatcherJson(
      g: Game,
      p: GamePlayer,
      user: GameUser,
      withFlags: WithFlags
  ): JsObject =
    Json
      .obj(
        "color" -> p.color.name,
        "name"  -> p.name
      )
      .add("user" -> user.match
        case Some(User.WithPerf(user, perf)) =>
          userJsonView.roundPlayer(user, withFlags.rating.option(Perf.Typed(perf, g.perfType))).some
        case _ if p.hasUser => userJsonView.ghost.some
        case _              => none
      )
      .add("ai" -> p.aiLevel)
      .add("rating" -> p.rating.ifTrue(withFlags.rating))
      .add("ratingDiff" -> p.ratingDiff.ifTrue(withFlags.rating))
      .add("provisional" -> (p.provisional.yes && withFlags.rating))
      .add("checks" -> checkCount(g, p.color))
      .add("berserk" -> p.berserk)
      .add("blurs" -> (withFlags.blurs so blurs(g, p)))

  def watcherJson(
      pov: Pov,
      users: GameUsers,
      pref: Option[Pref],
      me: Option[UserId],
      tv: Option[OnTv],
      initialFen: Option[Fen.Epd] = None,
      flags: WithFlags
  ) =
    getSocketStatus(pov.game).map: socket =>
      import pov.*
      Json
        .obj(
          "game" -> gameJsonView
            .baseWithChessDenorm(game, initialFen)
            .add("moveCentis" -> (flags.movetimes so game.moveTimes.map(_.map(_.centis))))
            .add("division" -> flags.division.option(divider(game, initialFen)))
            .add("opening" -> game.opening)
            .add("importedBy" -> game.pgnImport.flatMap(_.user)),
          "clock"          -> game.clock.map(clockJson),
          "correspondence" -> game.correspondenceClock,
          "player" -> {
            commonWatcherJson(game, player, users(pov.color), flags) ++ Json
              .obj(
                "version"   -> socket.version,
                "spectator" -> true
              )
              .add("id" -> flags.lichobileCompat.so(me.flatMap(game.player).map(_.id)))
          }.add("onGame" -> (player.isAi || socket.onGame(player.color))),
          "opponent" -> commonWatcherJson(game, opponent, users(pov.opponent.color), flags).add(
            "onGame" -> (opponent.isAi || socket.onGame(opponent.color))
          ),
          "orientation" -> pov.color.name,
          "url" -> flags.lichobileCompat.option:
            Json.obj(
              "socket" -> s"/watch/$gameId/${color.name}/v${ApiVersion.lichobile}",
              "round"  -> s"/$gameId/${color.name}"
            )
          ,
          "pref" -> pref.map: pref =>
            Json
              .obj(
                "animationDuration" -> animationMillis(pov, pref),
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
              .add("showCaptured" -> pref.captured)
        )
        .add("tv" -> tv.collect { case OnTv.Lichess(channel, flip) =>
          Json.obj("channel" -> channel, "flip" -> flip)
        })
        .add("userTv" -> tv.collect { case OnTv.User(userId) =>
          Json.obj("id" -> userId)
        })

  def replayJson(pov: Pov, pref: Pref, initialFen: Option[Fen.Epd]) =
    pov.game.whitePlayer.userId.so(lightUserGet) zip pov.game.blackPlayer.userId.so(lightUserGet) map {
      case (white, black) =>
        import pov.*
        import LightUser.lightUserWrites
        Json
          .obj(
            "game" -> {
              gameJsonView.baseWithChessDenorm(game, initialFen) ++ Json.obj(
                "pgn" -> pov.game.sans.mkString(" ")
              )
            },
            "white"       -> Json.obj("user" -> white),
            "black"       -> Json.obj("user" -> black),
            "orientation" -> pov.color.name,
            "pref" -> Json
              .obj(
                "animationDuration" -> animationMillis(pov, pref),
                "coords"            -> pref.coords
              )
              .add("highlight" -> pref.highlight)
          )
          .add("clock", game.clock.map(clockJson))
          .add("correspondence", game.correspondenceClock)
    }

  def userAnalysisJson(
      pov: Pov,
      pref: Pref,
      initialFen: Option[Fen.Epd],
      orientation: chess.Color,
      owner: Boolean,
      division: Option[chess.Division] = None
  ) =
    import pov.*
    val fen = Fen write game.chess
    Json
      .obj(
        "game" -> Json
          .obj(
            "id"      -> gameId,
            "variant" -> game.variant,
            "opening" -> game.opening,
            "fen"     -> fen,
            "turns"   -> game.ply,
            "player"  -> game.turnColor.name,
            "status"  -> game.status
          )
          .add("initialFen", initialFen)
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
            "animationDuration" -> animationMillis(pov, pref),
            "coords"            -> pref.coords,
            "moveEvent"         -> pref.moveEvent,
            "showCaptured"      -> pref.captured
          )
          .add("rookCastle" -> (pref.rookCastle == Pref.RookCastle.YES))
          .add("is3d" -> pref.is3d)
          .add("highlight" -> pref.highlight)
          .add("destination" -> (pref.destination && !pref.isBlindfold)),
        "userAnalysis" -> true
      )

  private def blurs(game: Game, player: lila.game.Player) =
    player.blurs.nonEmpty option {
      Json.toJsObject(player.blurs) +
        ("percent" -> JsNumber(game.playerBlurPercent(player.color)))
    }

  private[round] def clockJson(clock: Clock): JsObject =
    Json.toJsObject(clock) + ("moretime" -> JsNumber(actorApi.round.Moretime.defaultDuration.toSeconds))

  private def possibleMoves(pov: Pov): Option[JsValue] =
    pov.game.playableBy(pov.player) option
      lila.game.Event.PossibleMoves.json(pov.game.situation.destinations)

  private def possibleDrops(pov: Pov): Option[JsValue] =
    (pov.game playableBy pov.player).so:
      pov.game.situation.drops.map: drops =>
        JsString(drops.map(_.key).mkString)

  private def animationMillis(pov: Pov, pref: Pref) =
    pref.animationMillis * {
      if pov.game.finished then 1
      else math.max(0, math.min(1.2, ((pov.game.estimateTotalTime - 60) / 60) * 0.2))
    }

object JsonView:

  case class WithFlags(
      opening: Boolean = false,
      movetimes: Boolean = false,
      division: Boolean = false,
      clocks: Boolean = false,
      blurs: Boolean = false,
      rating: Boolean = true,
      puzzles: Boolean = false,
      nvui: Boolean = false,
      lichobileCompat: Boolean = false
  )
