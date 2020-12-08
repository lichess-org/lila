package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.i18n.defaultLang
import lila.tree.Node.{ minimalNodeJsonWriter, partitionTreeJsonWriter }
import chess.format.Forsyth

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(gameId: Game.ID, plies: Int, bc: Boolean): Fu[JsObject] =
    cache get CacheKey(gameId, plies, bc)

  // def noCache(game: Game, plies: Int): Fu[JsObject] =
  //   generate(game, plies)

  private case class CacheKey(gameId: Game.ID, plies: Int, bc: Boolean)

  private val cache = cacheApi[CacheKey, JsObject](1024, "puzzle.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(generate)
  }

  private def generate(ck: CacheKey): Fu[JsObject] =
    ck match {
      case CacheKey(gameId, plies, bc) =>
        gameRepo game gameId orFail s"Missing puzzle game $gameId!" flatMap { game =>
          lightUserApi preloadMany game.userIds map { _ =>
            if (bc) generateBc(game, plies)
            else generate(game, plies)
          }
        }
    }

  private def generate(game: Game, plies: Int): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "rated"   -> game.rated,
        "players" -> playersJson(game),
        "pgn"     -> game.chess.pgnMoves.take(plies + 1)
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
    Json.obj(
      "userId" -> p.userId,
      "name"   -> lila.game.Namer.playerTextBlocking(p, withRating = true)(lightUserApi.sync),
      "color"  -> p.color.name
    )
  })

  private def generateBc(game: Game, plies: Int): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "players" -> playersJson(game),
        "rated"   -> game.rated,
        "treeParts" -> {
          val pgnMoves = game.pgnMoves take plies
          for {
            pgnMove <- pgnMoves.lastOption
            situation <- chess.Replay
              .situations(pgnMoves, None, game.variant)
              .valueOr { err =>
                sys.error(s"GameJson.generateBc ${game.id} $err")
              }
              .lastOption
            uciMove <- situation.board.history.lastMove
          } yield Json.obj(
            "fen" -> Forsyth.>>(situation).value,
            "ply" -> plies,
            "san" -> pgnMove,
            "uci" -> uciMove.uci
          )
        }
      )
      .add("clock", game.clock.map(_.config.show))
}
