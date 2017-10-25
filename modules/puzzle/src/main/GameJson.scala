package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.tree.Node.{ partitionTreeJsonWriter, minimalNodeJsonWriter }

private final class GameJson(
    asyncCache: lila.memo.AsyncCache.Builder,
    lightUserApi: lila.user.LightUserApi
) {

  def apply(gameId: Game.ID, plies: Int, onlyLast: Boolean): Fu[JsObject] =
    cache get CacheKey(gameId, plies, onlyLast)

  private case class CacheKey(gameId: Game.ID, plies: Int, onlyLast: Boolean)

  private val cache = asyncCache.multi[CacheKey, JsObject](
    name = "puzzle.gameJson",
    f = generate,
    expireAfter = _.ExpireAfterAccess(1 hour),
    maxCapacity = 1024
  )

  private def generate(ck: CacheKey): Fu[JsObject] = ck match {
    case CacheKey(gameId, plies, onlyLast) => for {
      game <- (GameRepo game gameId).flatten(s"Missing puzzle game $gameId!")
      _ <- lightUserApi preloadMany game.userIds
      perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
      tree = TreeBuilder(game, plies)
    } yield Json.obj(
      "id" -> game.id,
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
      "treeParts" -> onlyLast.fold(
        tree.mainlineNodeList.lastOption.map(minimalNodeJsonWriter.writes),
        partitionTreeJsonWriter.writes(tree).some
      )
    ).add("clock", game.clock.map(_.config.show))
  }
}
