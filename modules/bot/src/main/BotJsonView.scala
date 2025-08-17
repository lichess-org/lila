package lila.bot

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.game.{ Game, GameRepo, Pov, WithInitialFen }
import lila.core.i18n.Translate
import lila.game.GameExt.perfType
import lila.game.JsonView.given

final class BotJsonView(
    lightUserApi: lila.core.user.LightUserApi,
    gameRepo: GameRepo,
    rematches: lila.game.Rematches
)(using Executor):

  def gameFull(game: Game)(using Translate): Fu[JsObject] = gameRepo.withInitialFen(game).flatMap(gameFull)

  def gameFull(wf: WithInitialFen)(using Translate): Fu[JsObject] =
    gameState(wf).map: state =>
      gameImmutable(wf) ++ Json.obj(
        "type" -> "gameFull",
        "state" -> state
      )

  def gameImmutable(wf: WithInitialFen)(using Translate): JsObject =
    import wf.*
    Json
      .obj(
        "id" -> game.id,
        "variant" -> game.variant,
        "speed" -> game.speed.key,
        "perf" -> Json.obj("name" -> game.perfType.trans),
        "rated" -> game.rated,
        "createdAt" -> game.createdAt,
        "white" -> playerJson(game.pov(Color.white)),
        "black" -> playerJson(game.pov(Color.black)),
        "initialFen" -> fen.fold("startpos")(_.value)
      )
      .add("clock" -> game.clock.map(_.config))
      .add("daysPerTurn" -> game.daysPerTurn)
      .add("tournamentId" -> game.tournamentId)

  def gameState(wf: WithInitialFen): Fu[JsObject] =
    import wf.*
    chess.format.UciDump(game.sans, fen, game.variant).toFuture.map { uciMoves =>
      Json
        .obj(
          "type" -> "gameState",
          "moves" -> uciMoves.mkString(" "),
          "wtime" -> millisRemaining(game, Color.white),
          "btime" -> millisRemaining(game, Color.black),
          "winc" -> game.clock.so[Long](_.config.increment.millis),
          "binc" -> game.clock.so[Long](_.config.increment.millis),
          "status" -> game.status.name
        )
        .add("wdraw" -> game.whitePlayer.isOfferingDraw)
        .add("bdraw" -> game.blackPlayer.isOfferingDraw)
        .add("wtakeback" -> game.whitePlayer.isProposingTakeback)
        .add("btakeback" -> game.blackPlayer.isProposingTakeback)
        .add("winner" -> game.winnerColor)
        .add("rematch" -> rematches.getAcceptedId(game.id))
    }

  private def millisRemaining(game: Game, color: Color): Int =
    game.clock
      .map(_.remainingTime(color).millis.toInt)
      .orElse(game.correspondenceClock.map(_.remainingTime(color).toInt * 1000))
      .getOrElse(Int.MaxValue)

  def chatLine(username: UserName, text: String, player: Boolean) =
    Json.obj(
      "type" -> "chatLine",
      "room" -> (if player then "player" else "spectator"),
      "username" -> username,
      "text" -> text
    )

  def opponentGoneClaimIn(seconds: Int) = Json.obj(
    "type" -> "opponentGone",
    "gone" -> true,
    "claimWinInSeconds" -> seconds
  )
  def opponentGoneIsBack = Json.obj(
    "type" -> "opponentGone",
    "gone" -> false
  )

  private def playerJson(pov: Pov) =
    val light = pov.player.userId.flatMap(lightUserApi.sync)
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
      "initial" -> c.limit.millis,
      "increment" -> c.increment.millis
    )
