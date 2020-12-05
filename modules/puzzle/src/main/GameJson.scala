package lila.puzzle

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo, PerfPicker }
import lila.i18n.defaultLang
import lila.tree.Node.{ minimalNodeJsonWriter, partitionTreeJsonWriter }

final private class GameJson(
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi,
    lightUserApi: lila.user.LightUserApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  def apply(gameId: Game.ID, plies: Int): Fu[JsObject] =
    cache get CacheKey(gameId, plies)

  def noCache(game: Game, plies: Int): Fu[JsObject] =
    generate(game, plies)

  private case class CacheKey(gameId: Game.ID, plies: Int)

  private val cache = cacheApi[CacheKey, JsObject](1024, "puzzle.gameJson") {
    _.expireAfterAccess(5 minutes)
      .maximumSize(1024)
      .buildAsyncFuture(generate)
  }

  private def generate(ck: CacheKey): Fu[JsObject] =
    ck match {
      case CacheKey(gameId, plies) =>
        gameRepo game gameId orFail s"Missing puzzle game $gameId!" flatMap {
          generate(_, plies)
        }
    }

  private def generate(game: Game, plies: Int): Fu[JsObject] =
    lightUserApi preloadMany game.userIds map { _ =>
      val perfType = lila.rating.PerfType orDefault PerfPicker.key(game)
      val tree     = treeBuilder(game, plies)
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
          "pgn" -> game.chess.pgnMoves.take(plies + 1)
        )
        .add("clock", game.clock.map(_.config.show))
    }

  private def treeBuilder(game: Game, plies: Int): lila.tree.Root = {
    import chess.format.{ Forsyth, Uci, UciCharPair }
    chess.Replay.gameMoveWhileValid(game.pgnMoves take plies, Forsyth.initial, game.variant) match {
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val fen = Forsyth >> init
        val root = lila.tree.Root(
          ply = init.turns,
          fen = fen,
          check = init.situation.check,
          crazyData = None
        )
        def makeBranch(g: chess.Game, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          lila.tree.Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            check = g.situation.check,
            crazyData = None
          )
        }
        games.reverse match {
          case Nil => root
          case (g, m) :: rest =>
            root prependChild rest.foldLeft(makeBranch(g, m)) { case (node, (g, m)) =>
              makeBranch(g, m) prependChild node
            }
        }
    }
  }

  private val logChessError = (id: Game.ID) =>
    (err: String) =>
      logger.warn(s"TreeBuilder https://lichess.org/$id ${err.linesIterator.toList.headOption}")
}
