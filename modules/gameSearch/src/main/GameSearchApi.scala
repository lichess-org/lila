package lila.gameSearch

import lila.common.PimpedJson._
import lila.game.actorApi._
import lila.game.{ Game, GameRepo }
import lila.search._

import play.api.libs.json._

final class GameSearchApi(client: ESClient) extends SearchReadApi[Game, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      import lila.db.api.$find
      import lila.game.tube.gameTube
      $find.byOrderedIds[lila.game.Game](res.ids)
    }

  def count(query: Query) =
    client.count(query) map (_.count)

  def store(game: Game) = storable(game) ?? {
    GameRepo isAnalysed game.id flatMap { analysed =>
      client.store(Id(game.id), toDoc(game, analysed))
    }
  }

  private def storable(game: Game) = (game.finished || game.imported) && game.playedTurns > 4

  private def toDoc(game: Game, analysed: Boolean) = Json.obj(
    Fields.status -> (game.status match {
      case s if s.is(_.Timeout) => chess.Status.Resign
      case s if s.is(_.NoStart) => chess.Status.Resign
      case s                    => game.status
    }).id,
    Fields.turns -> math.ceil(game.turns.toFloat / 2),
    Fields.rated -> game.rated,
    Fields.variant -> game.variant.id,
    Fields.uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
    Fields.winner -> (game.winner flatMap (_.userId)),
    Fields.winnerColor -> game.winner.fold(3)(_.color.fold(1, 2)),
    Fields.averageRating -> game.averageUsersRating,
    Fields.ai -> game.aiLevel,
    Fields.date -> (lila.search.Date.formatter print game.createdAt),
    Fields.duration -> game.estimateTotalTime,
    Fields.opening -> (game.opening map (_.code.toLowerCase)),
    Fields.analysed -> analysed,
    Fields.whiteUser -> game.whitePlayer.userId,
    Fields.blackUser -> game.blackPlayer.userId
  ).noNull

  def reset = client.putMapping >> {
    import lila.db.api._
    import lila.game.tube.gameTube
    var nb = 0
    var nbSkipped = 0
    var started = nowMillis
    for {
      size <- $count($select.all)
      batchSize = 1000
      limit = 20 * 1000
      _ <- $enumerate.bulk[Option[Game]]($query.all, batchSize, limit) { gameOptions =>
        val games = gameOptions.flatten filter storable
        val nbGames = games.size
        (GameRepo filterAnalysed games.map(_.id).toSeq flatMap { analysedIds =>
          client.storeBulk(games map { g =>
            Id(g.id) -> toDoc(g, analysedIds(g.id))
          })
        }) >>- {
          nb = nb + nbGames
          nbSkipped = nbSkipped + gameOptions.size - nbGames
          val perS = (batchSize * 1000) / math.max(1, (nowMillis - started))
          started = nowMillis
          loginfo("[game search] Indexed %d of %d, skipped %d, at %d/s".format(nb, size, nbSkipped, perS))
        }
      }
    } yield ()
  }
}
