package lila.puzzle

import chess.Ply
import chess.format.{ Fen, UciCharPair }
import play.api.libs.json.*

import lila.game.{ Game, GameRepo }
import lila.i18n.defaultLang
import lila.common.Json.given

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(using Executor):

  def apply(gameId: GameId, plies: Ply, bc: Boolean): Fu[JsObject] =
    (if bc then bcCache else cache) get writeKey(gameId, plies)

  def noCache(game: Game, plies: Ply): Fu[JsObject] =
    lightUserApi preloadMany game.userIds inject generate(game, plies)

  def noCacheBc(game: Game, plies: Ply): Fu[JsObject] =
    lightUserApi preloadMany game.userIds inject generateBc(game, plies)

  private def readKey(k: String): (GameId, Ply) =
    k.drop(GameId.size).toIntOption match
      case Some(ply) => (GameId take k, Ply(ply))
      case _         => sys error s"puzzle.GameJson invalid key: $k"
  private def writeKey(id: GameId, ply: Ply) = s"$id$ply"

  private val cache = cacheApi[String, JsObject](4096, "puzzle.gameJson"):
    _.expireAfterAccess(5 minutes)
      .maximumSize(4096)
      .buildAsyncFuture: key =>
        val (id, plies) = readKey(key)
        generate(id, plies, false)

  private val bcCache = cacheApi[String, JsObject](64, "puzzle.bc.gameJson"):
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture: key =>
        val (id, plies) = readKey(key)
        generate(id, plies, true)

  private def generate(gameId: GameId, plies: Ply, bc: Boolean): Fu[JsObject] =
    gameRepo gameFromSecondary gameId orFail s"Missing puzzle game $gameId!" flatMap { game =>
      lightUserApi preloadMany game.userIds map { _ =>
        if bc then generateBc(game, plies)
        else generate(game, plies)
      }
    }

  private def generate(game: Game, plies: Ply): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "rated"   -> game.rated,
        "players" -> playersJson(game),
        "pgn"     -> game.chess.sans.take(plies.value + 1).mkString(" ")
      )
      .add("clock", game.clock.map(_.config.show))

  private def perfJson(game: Game) =
    Json.obj(
      "key"  -> game.perfType.key,
      "name" -> game.perfType.trans(using defaultLang)
    )

  private def playersJson(game: Game) = JsArray(game.players.mapList { p =>
    val user = p.userId.fold(lila.common.LightUser.ghost)(lightUserApi.syncFallback)
    Json
      .obj(
        "userId" -> user.id,
        "name"   -> s"${user.name}${p.rating.so(r => s" ($r)")}",
        "color"  -> p.color.name
      )
      .add("title" -> user.title)
  })

  private def generateBc(game: Game, plies: Ply): JsObject =
    Json
      .obj(
        "id"      -> game.id,
        "perf"    -> perfJson(game),
        "players" -> playersJson(game),
        "rated"   -> game.rated,
        "treeParts" -> {
          val pgnMoves = game.sans.take(plies.value + 1)
          for
            pgnMove <- pgnMoves.lastOption
            situation <- chess.Replay
              .situations(pgnMoves, None, game.variant)
              .valueOr: err =>
                sys.error(s"GameJson.generateBc ${game.id} $err")
              .lastOption
            uciMove <- situation.board.history.lastMove
          yield Json.obj(
            "fen" -> Fen.write(situation).value,
            "ply" -> (plies + 1),
            "san" -> pgnMove,
            "id"  -> UciCharPair(uciMove).toString,
            "uci" -> uciMove.uci
          )
        }
      )
      .add("clock", game.clock.map(_.config.show))
