package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.tree.Node.partitionTreeJsonWriter

object GameJson {

  import lila.game.JsonView._

  private val cache = lila.memo.AsyncCache[Game.ID, JsObject](
    name = "puzzle.gameJson",
    f = generate,
    maxCapacity = 500)

  def apply(gameId: Game.ID): Fu[JsObject] = cache(gameId)

  def generate(gameId: Game.ID): Fu[JsObject] =
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
        "treeParts" -> partitionTreeJsonWriter.writes(TreeBuilder(game))
      ).noNull
    }
}
