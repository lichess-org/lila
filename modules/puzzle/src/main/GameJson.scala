package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.tree.Node.partitionTreeJsonWriter

object GameJson {

  import lila.game.JsonView._

  case class CacheKey(gameId: Game.ID, plies: Int)

  private val cache = lila.memo.AsyncCache[CacheKey, JsObject](
    name = "puzzle.gameJson",
    f = generate,
    maxCapacity = 500)

  def apply(gameId: Game.ID, plies: Int): Fu[JsObject] = cache(CacheKey(gameId, plies))

  def generate(ck: CacheKey): Fu[JsObject] = ck match {
    case CacheKey(gameId, plies) =>
      (GameRepo game gameId).flatten(s"Missing puzzle game $gameId!") map { game =>
        Json.obj(
          "id" -> game.id,
          "speed" -> game.speed.key,
          "perf" -> PerfPicker.key(game),
          "rated" -> game.rated,
          "winner" -> game.winnerColor.map(_.name),
          "turns" -> game.turns,
          "status" -> game.status,
          "tournamentId" -> game.tournamentId,
          "createdAt" -> game.createdAt,
          "treeParts" -> partitionTreeJsonWriter.writes(TreeBuilder(game, plies))
        ).noNull
      }
  }
}
