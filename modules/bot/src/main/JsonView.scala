package lila.bot

import play.api.libs.json._

import chess.format.FEN

import lila.game.JsonView._
import lila.game.{ Game, Pov, GameRepo }

final class BotJsonView(
    lightUserApi: lila.user.LightUserApi
) {

  def gameFull(game: Game): Fu[JsObject] = for {
    initialFen <- GameRepo.initialFen(game) map2 FEN.apply
    immutable = gameImmutable(game, initialFen)
    state <- gameState(game, initialFen)
  } yield immutable + ("state" -> state)

  def gameImmutable(game: Game, initialFen: Option[FEN]): JsObject = Json.obj(
    "id" -> game.id,
    "variant" -> game.variant,
    "speed" -> game.speed.key,
    "perf" -> lila.game.PerfPicker.key(game),
    "rated" -> game.rated,
    "createdAt" -> game.createdAt,
    "white" -> playerJson(game.whitePov),
    "black" -> playerJson(game.blackPov),
    "initialFen" -> initialFen.fold("startpos")(_.value)
  )
    .add("tournamentId" -> game.tournamentId)

  def gameState(game: Game, initialFen: Option[FEN]): Fu[JsObject] = for {
    uciMoves ← chess.format.UciDump(game.pgnMoves, initialFen.map(_.value), game.variant).future
  } yield Json.obj(
    "moves" -> uciMoves.mkString(" "),
    "wtime" -> millisOf(game.whitePov),
    "btime" -> millisOf(game.blackPov),
    "winc" -> game.clock.??(_.config.increment.millis),
    "binc" -> game.clock.??(_.config.increment.millis)
  )
    .add("rematch" -> game.next)

  private def playerJson(pov: Pov) = {
    val light = pov.player.userId flatMap lightUserApi.sync
    Json.obj()
      .add("id" -> light.map(_.id))
      .add("name" -> light.map(_.name))
      .add("title" -> light.map(_.title))
      .add("rating" -> pov.player.rating)
      .add("provisional" -> pov.player.provisional)
  }

  private def millisOf(pov: Pov): Int =
    pov.game.clock.fold(Int.MaxValue)(_.remainingTime(pov.color).millis.toInt)
}
