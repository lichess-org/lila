package lila.puzzle

import chess.Ply
import chess.format.{ Fen, UciCharPair }
import play.api.libs.json.*

import lila.common.Json.given
import lila.core.LightUser

final private class GameJson(
    gameRepo: lila.core.game.GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.core.user.LightUserApi
)(using Executor, lila.core.i18n.Translator):

  given play.api.i18n.Lang = lila.core.i18n.defaultLang

  def apply(gameId: GameId, plies: Ply, bc: Boolean): Fu[JsObject] =
    (if bc then bcCache else cache).get(writeKey(gameId, plies))

  def noCache(game: Game, plies: Ply): Fu[JsObject] =
    lightUserApi.preloadMany(game.userIds).inject(generate(game, plies))

  def noCacheBc(game: Game, plies: Ply): Fu[JsObject] =
    lightUserApi.preloadMany(game.userIds).inject(generateBc(game, plies))

  private def readKey(k: String): (GameId, Ply) =
    k.drop(GameId.size).toIntOption match
      case Some(ply) => (GameId.take(k), Ply(ply))
      case _ => sys.error(s"puzzle.GameJson invalid key: $k")
  private def writeKey(id: GameId, ply: Ply) = s"$id$ply"

  private val cache = cacheApi[String, JsObject](4096, "puzzle.gameJson"):
    _.expireAfterAccess(5.minutes)
      .maximumSize(4096)
      .buildAsyncFuture: key =>
        val (id, plies) = readKey(key)
        generate(id, plies, false)

  private val bcCache = cacheApi[String, JsObject](1024, "puzzle.bc.gameJson"):
    _.expireAfterAccess(5.minutes)
      .maximumSize(1024)
      .buildAsyncFuture: key =>
        val (id, plies) = readKey(key)
        generate(id, plies, true)

  private def generate(gameId: GameId, plies: Ply, bc: Boolean): Fu[JsObject] =
    gameRepo.gameFromSecondary(gameId).orFail(s"Missing puzzle game $gameId!").flatMap { game =>
      lightUserApi
        .preloadMany(game.userIds)
        .inject:
          if bc then generateBc(game, plies)
          else generate(game, plies)
    }

  private def generate(game: Game, plies: Ply): JsObject =
    Json
      .obj(
        "id" -> game.id,
        "perf" -> perfJson(game),
        "rated" -> game.rated,
        "players" -> playersJson(game),
        "pgn" -> game.chess.sans.take(plies.value + 1).mkString(" ")
      )
      .add("clock", game.clock.map(_.config.show))

  private def perfJson(game: Game) =
    Json.obj(
      "key" -> game.perfKey,
      "name" -> lila.rating.PerfType(game.perfKey).trans
    )

  private def playersJson(game: Game) = JsArray(game.players.mapList: p =>
    val user = p.userId.fold(LightUser.ghost)(lightUserApi.syncFallback)
    Json.toJsObject(user) ++
      Json
        .obj("color" -> p.color.name)
        .add("rating" -> p.rating))

  private def generateBc(game: Game, plies: Ply): JsObject =
    Json
      .obj(
        "id" -> game.id,
        "perf" -> perfJson(game),
        "players" -> playersJson(game),
        "rated" -> game.rated,
        "treeParts" -> {
          val pgnMoves = game.sans.take(plies.value + 1)
          for
            pgnMove <- pgnMoves.lastOption
            position =
              game.variant.initialPosition
                .forward(pgnMoves)
                .valueOr: err =>
                  sys.error(s"GameJson.generateBc ${game.id} $err")
            uciMove <- position.history.lastMove
          yield Json.obj(
            "fen" -> Fen.write(position).value,
            "ply" -> (plies + 1),
            "san" -> pgnMove,
            "id" -> UciCharPair(uciMove).toString,
            "uci" -> uciMove.uci
          )
        }
      )
      .add("clock", game.clock.map(_.config.show))
