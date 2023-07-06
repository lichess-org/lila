package lila.bot

import play.api.i18n.Lang
import play.api.libs.json.*

import lila.common.Json.given
import lila.game.JsonView.given
import lila.game.{ Game, GameRepo, Pov }

final class BotJsonView(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: GameRepo,
    rematches: lila.game.Rematches
)(using Executor):

  def gameFull(game: Game)(using Lang): Fu[JsObject] = gameRepo.withInitialFen(game) flatMap gameFull

  def gameFull(wf: Game.WithInitialFen)(using Lang): Fu[JsObject] =
    gameState(wf) map { state =>
      gameImmutable(wf) ++ Json.obj(
        "type"  -> "gameFull",
        "state" -> state
      )
    }

  def gameImmutable(wf: Game.WithInitialFen)(using Lang): JsObject =
    import wf.*
    Json
      .obj(
        "id"         -> game.id,
        "variant"    -> game.variant,
        "speed"      -> game.speed.key,
        "perf"       -> Json.obj("name" -> game.perfType.trans),
        "rated"      -> game.rated,
        "createdAt"  -> game.createdAt,
        "white"      -> playerJson(game.whitePov),
        "black"      -> playerJson(game.blackPov),
        "initialFen" -> fen.fold("startpos")(_.value)
      )
      .add("clock" -> game.clock.map(_.config))
      .add("daysPerTurn" -> game.daysPerTurn)
      .add("tournamentId" -> game.tournamentId)

  def gameState(wf: Game.WithInitialFen): Fu[JsObject] =
    import wf.*
    chess.format.UciDump(game.sans, fen, game.variant).toFuture map { uciMoves =>
      Json
        .obj(
          "type"   -> "gameState",
          "moves"  -> uciMoves.mkString(" "),
          "wtime"  -> game.whitePov.millisRemaining,
          "btime"  -> game.blackPov.millisRemaining,
          "winc"   -> (game.clock.so(_.config.increment.millis): Long),
          "binc"   -> (game.clock.so(_.config.increment.millis): Long),
          "status" -> game.status.name
        )
        .add("wdraw" -> game.whitePlayer.isOfferingDraw)
        .add("bdraw" -> game.blackPlayer.isOfferingDraw)
        .add("wtakeback" -> game.whitePlayer.isProposingTakeback)
        .add("btakeback" -> game.blackPlayer.isProposingTakeback)
        .add("winner" -> game.winnerColor)
        .add("rematch" -> rematches.getAcceptedId(game.id))
    }

  def chatLine(username: UserName, text: String, player: Boolean) =
    Json.obj(
      "type"     -> "chatLine",
      "room"     -> (if player then "player" else "spectator"),
      "username" -> username,
      "text"     -> text
    )

  def opponentGoneClaimIn(seconds: Int) = Json.obj(
    "type"              -> "opponentGone",
    "gone"              -> true,
    "claimWinInSeconds" -> seconds
  )
  def opponentGoneIsBack = Json.obj(
    "type" -> "opponentGone",
    "gone" -> false
  )

  private def playerJson(pov: Pov) =
    val light = pov.player.userId flatMap lightUserApi.sync
    Json
      .obj()
      .add("aiLevel" -> pov.player.aiLevel)
      .add("id" -> light.map(_.id))
      .add("name" -> light.map(_.name))
      .add("title" -> light.map(_.title))
      .add("rating" -> pov.player.rating)
      .add("provisional" -> pov.player.provisional)

  private given OWrites[chess.Clock.Config] = OWrites: c =>
    Json.obj(
      "initial"   -> c.limit.millis,
      "increment" -> c.increment.millis
    )
