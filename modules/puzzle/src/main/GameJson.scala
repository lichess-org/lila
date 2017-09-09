package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.common.PimpedJson._
import lila.game.{ Game, GameRepo, PerfPicker }
import lila.tree.Node.partitionTreeJsonWriter

private final class GameJson(
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi
) {

  case class CacheKey(gameId: Game.ID, plies: Int)

  private val cache = asyncCache.multi[CacheKey, JsObject](
    name = "puzzle.gameJson",
    f = generate,
    expireAfter = _.ExpireAfterAccess(1 hour),
    maxCapacity = 1024
  )

  def apply(gameId: Game.ID, plies: Int): Fu[JsObject] = cache get CacheKey(gameId, plies)

  def generate(ck: CacheKey): Fu[JsObject] = ck match {
    case CacheKey(gameId, plies) => for {
      game <- (GameRepo game gameId).flatten(s"Missing puzzle game $gameId!")
      _ <- lightUserApi preloadMany game.userIds
      perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
      tree = TreeBuilder(game, plies)
    } yield Json.obj(
      "id" -> game.id,
      "clock" -> game.clock.map(_.config.show),
      "perf" -> Json.obj(
        "icon" -> perfType.iconChar.toString,
        "name" -> perfType.name
      ),
      "rated" -> game.rated,
      "players" -> JsArray(game.players.map { p =>
        Json.obj(
          "userId" -> p.userId,
          "name" -> lila.game.Namer.playerText(p, withRating = true)(lightUserApi.sync),
          "color" -> p.color.name
        )
      }),
      "treeParts" -> partitionTreeJsonWriter.writes(tree)
    ).noNull
  }
}
