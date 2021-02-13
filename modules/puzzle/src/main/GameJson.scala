package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.tree.Node.{ minimalNodeJsonWriter, partitionTreeJsonWriter }
import lila.i18n.defaultLang

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(gameId: Game.ID, plies: Int, onlyLast: Boolean): Fu[JsObject] =
    cache get CacheKey(gameId, plies, onlyLast)

  def noCache(game: Game, plies: Int, onlyLast: Boolean): Fu[JsObject] =
    generate(game, plies, onlyLast)

  private case class CacheKey(gameId: Game.ID, plies: Int, onlyLast: Boolean)

  private val cache = cacheApi[CacheKey, JsObject](1024, "puzzle.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(generate)
  }

  private def generate(ck: CacheKey): Fu[JsObject] =
    ck match {
      case CacheKey(gameId, plies, onlyLast) =>
        gameRepo game gameId orFail s"Missing puzzle game $gameId!" flatMap {
          generate(_, plies, onlyLast)
        }
    }

  private def generate(game: Game, plies: Int, onlyLast: Boolean): Fu[JsObject] =
    lightUserApi preloadMany game.userIds map { _ =>
      val perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
      val tree     = TreeBuilder(game, plies)
      Json
        .obj(
          "id" -> game.id,
          "perf" -> Json.obj(
            "icon" -> perfType.iconChar.toString,
            "name" -> perfType.trans(defaultLang)
          ),
          "rated" -> game.rated,
          "players" -> JsArray(game.players.map { p =>
            Json.obj(
              "userId" -> p.userId,
              "name"   -> lila.game.Namer.playerTextBlocking(p, withRating = true)(lightUserApi.sync),
              "color"  -> p.color.name
            )
          }),
          "treeParts" -> {
            if (onlyLast) tree.mainlineNodeList.lastOption.map(minimalNodeJsonWriter.writes)
            else partitionTreeJsonWriter.writes(tree).some
          }
        )
        .add("clock", game.clock.map(_.config.show))
    }
}
