package lila.puzzle

import lila.common.Json._
import shogi.format.usi.UsiCharPair
import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.i18n.defaultLang

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(gameId: Game.ID, plies: Int, bc: Boolean): Fu[JsObject] =
    (if (bc) bcCache else cache) get writeKey(gameId, plies)

  def noCacheBc(game: Game, plies: Int): Fu[JsObject] =
    lightUserApi preloadMany game.userIds map { _ =>
      generateBc(game, plies)
    }

  private def readKey(k: String): (Game.ID, Int) =
    k.drop(Game.gameIdSize).toIntOption match {
      case Some(ply) => (k take Game.gameIdSize, ply)
      case _         => sys error s"puzzle.GameJson invalid key: $k"
    }
  private def writeKey(id: Game.ID, ply: Int) = s"$id$ply"

  private val cache = cacheApi[String, JsObject](4096, "puzzle.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(key =>
        readKey(key) match {
          case (id, plies) => generate(id, plies, false)
        }
      )
  }

  private val bcCache = cacheApi[String, JsObject](64, "puzzle.bc.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(key =>
        readKey(key) match {
          case (id, plies) => generate(id, plies, true)
        }
      )
  }

  private def generate(gameId: Game.ID, plies: Int, bc: Boolean): Fu[JsObject] =
    gameRepo game gameId orFail s"Missing puzzle game $gameId!" flatMap { game =>
      lightUserApi preloadMany game.userIds map { _ =>
        if (bc) generateBc(game, plies)
        else generate(game, plies)
      }
    }

  private def generate(game: Game, plies: Int): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "rated"   -> game.rated,
        "players" -> playersJson(game),
        "moves"   -> game.shogi.usiMoves.take(plies + 1).map(_.usi).mkString(" ")
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
  })

  private def generateBc(game: Game, plies: Int): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "players" -> playersJson(game),
        "rated"   -> game.rated,
        "treeParts" -> {
          val usiMoves = game.usiMoves.take(plies + 1)
          for {
            usi <- usiMoves.lastOption
            situation = shogi.Replay
              .situations(usiMoves, None, shogi.variant.Standard)
              .valueOr { err =>
                sys.error(s"GameJson.generateBc ${game.id} $err")
              }
              .last
          } yield Json.obj(
            "sfen" -> situation.toSfen,
            "ply"  -> (plies + 1),
            "usi"  -> usi.usi,
            "id"   -> UsiCharPair(usi, shogi.variant.Standard).toString
          )
        }
      )
      .add("clock", game.clock.map(_.config.show))
}
