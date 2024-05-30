package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.i18n.defaultLang

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(gameId: Game.ID, plies: Int): Fu[JsObject] =
    cache get writeKey(gameId, plies)

  private def readKey(k: String): (Game.ID, Int) =
    k.drop(Game.gameIdSize).toIntOption match {
      case Some(ply) => (k take Game.gameIdSize, ply)
      case _         => sys error s"puzzle.GameJson invalid key: $k"
    }
  private def writeKey(id: Game.ID, ply: Int) = s"$id$ply"

  private val cache = cacheApi[String, JsObject](512, "puzzle.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(key =>
        readKey(key) match {
          case (id, plies) => generate(id, plies)
        }
      )
  }

  private def generate(gameId: Game.ID, plies: Int): Fu[JsObject] =
    gameRepo game gameId orFail s"Missing puzzle game $gameId!" flatMap { game =>
      lightUserApi preloadMany game.userIds map { _ =>
        gameJson(game, plies)
      }
    }

  private def gameJson(game: Game, plies: Int): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "rated"   -> game.rated,
        "players" -> playersJson(game),
        "moves"   -> game.shogi.usis.take(plies + 1).map(_.usi).mkString(" ")
      )
      .add("clock", game.clock.map(_.config.show))

  private def perfJson(game: Game) = {
    val perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
    Json.obj(
      "icon" -> perfType.iconChar.toString,
      "name" -> perfType.trans(defaultLang)
    )
  }

  private def playersJson(game: Game) = JsArray(game.players.map { p =>
    val userId = p.userId | "anon"
    val user   = lightUserApi.syncFallback(userId)
    Json
      .obj(
        "userId" -> userId,
        "name"   -> s"${user.name}${p.rating.??(r => s" ($r)")}",
        "color"  -> p.color.name
      )
      .add("title" -> user.title)
      .add("ai" -> p.aiLevel)
      .add("aiCode" -> p.aiCode)
  })

}
