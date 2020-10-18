package lila.bot

import play.api.i18n.Lang
import play.api.libs.json._

import lila.common.Json.jodaWrites
import lila.game.JsonView._
import lila.game.{ Game, GameRepo, Pov }

final class BotJsonView(
    lightUserApi: lila.user.LightUserApi,
    gameRepo: GameRepo,
    rematches: lila.game.Rematches
)(implicit ec: scala.concurrent.ExecutionContext) {

  def gameFull(game: Game)(implicit lang: Lang): Fu[JsObject] = gameRepo.withInitialFen(game) flatMap gameFull

  def gameFull(wf: Game.WithInitialFen)(implicit lang: Lang): Fu[JsObject] =
    gameState(wf) map { state =>
      gameImmutable(wf) ++ Json.obj(
        "type"  -> "gameFull",
        "state" -> state
      )
    }

  def gameImmutable(wf: Game.WithInitialFen)(implicit lang: Lang): JsObject = {
    import wf._
    Json
      .obj(
        "id"      -> game.id,
        "variant" -> game.variant,
        "clock"   -> game.clock.map(_.config),
        "speed"   -> game.speed.key,
        "perf" -> game.perfType.map { p =>
          Json.obj("name" -> p.trans)
        },
        "rated"      -> game.rated,
        "createdAt"  -> game.createdAt,
        "white"      -> playerJson(game.whitePov),
        "black"      -> playerJson(game.blackPov),
        "initialFen" -> fen.fold("startpos")(_.value)
      )
      .add("tournamentId" -> game.tournamentId)
  }

  def gameState(wf: Game.WithInitialFen): Fu[JsObject] = {
    import wf._
    chess.format.UciDump(game.pgnMoves, fen, game.variant).toFuture map { uciMoves =>
      Json
        .obj(
          "type"   -> "gameState",
          "moves"  -> uciMoves.mkString(" "),
          "wtime"  -> millisOf(game.whitePov),
          "btime"  -> millisOf(game.blackPov),
          "winc"   -> game.clock.??(_.config.increment.millis),
          "binc"   -> game.clock.??(_.config.increment.millis),
          "wdraw"  -> game.whitePlayer.isOfferingDraw,
          "bdraw"  -> game.blackPlayer.isOfferingDraw,
          "status" -> game.status.name
        )
        .add("winner" -> game.winnerColor)
        .add("rematch" -> rematches.of(game.id))
    }
  }

  def chatLine(username: String, text: String, player: Boolean) =
    Json.obj(
      "type"     -> "chatLine",
      "room"     -> (if (player) "player" else "spectator"),
      "username" -> username,
      "text"     -> text
    )

  private def playerJson(pov: Pov) = {
    val light = pov.player.userId flatMap lightUserApi.sync
    Json
      .obj()
      .add("aiLevel" -> pov.player.aiLevel)
      .add("id" -> light.map(_.id))
      .add("name" -> light.map(_.name))
      .add("title" -> light.map(_.title))
      .add("rating" -> pov.player.rating)
      .add("provisional" -> pov.player.provisional)
  }

  private def millisOf(pov: Pov): Int =
    pov.game.clock
      .map(_.remainingTime(pov.color).millis.toInt)
      .orElse(pov.game.correspondenceClock.map(_.remainingTime(pov.color).toInt * 1000))
      .getOrElse(Int.MaxValue)

  implicit private val clockConfigWriter: OWrites[chess.Clock.Config] = OWrites { c =>
    Json.obj(
      "initial"   -> c.limit.millis,
      "increment" -> c.increment.millis
    )
  }
}
