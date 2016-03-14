package lila.gameSearch

import lila.common.PimpedJson._
import lila.game.actorApi._
import lila.game.{ Game, GameRepo }
import lila.search._

import org.joda.time.DateTime
import play.api.libs.json._

final class GameSearchApi(client: ESClient) extends SearchReadApi[Game, Query] {

  private var writeable = true

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      import lila.db.api.$find
      import lila.game.tube.gameTube
      $find.byOrderedIds[lila.game.Game](res.ids)
    }

  def count(query: Query) =
    client.count(query) map (_.count)

  def ids(query: Query, max: Int): Fu[List[String]] =
    client.search(query, From(0), Size(max)).map(_.ids)

  def store(game: Game) = (writeable && storable(game)) ?? {
    GameRepo isAnalysed game.id flatMap { analysed =>
      client.store(Id(game.id), toDoc(game, analysed))
    }
  }

  private def storable(game: Game) = (game.finished || game.imported)

  private def toDoc(game: Game, analysed: Boolean) = Json.obj(
    Fields.status -> (game.status match {
      case s if s.is(_.Timeout) => chess.Status.Resign
      case s if s.is(_.NoStart) => chess.Status.Resign
      case s                    => game.status
    }).id,
    Fields.turns -> math.ceil(game.turns.toFloat / 2),
    Fields.rated -> game.rated,
    Fields.perf -> game.perfType.map(_.id),
    Fields.uids -> game.userIds.toArray.some.filterNot(_.isEmpty),
    Fields.winner -> (game.winner flatMap (_.userId)),
    Fields.winnerColor -> game.winner.fold(3)(_.color.fold(1, 2)),
    Fields.averageRating -> game.averageUsersRating,
    Fields.ai -> game.aiLevel,
    Fields.date -> (lila.search.Date.formatter print game.updatedAtOrCreatedAt),
    Fields.duration -> game.durationSeconds,
    Fields.clockInit -> game.clock.map(_.limit),
    Fields.clockInc -> game.clock.map(_.increment),
    Fields.analysed -> analysed,
    Fields.whiteUser -> game.whitePlayer.userId,
    Fields.blackUser -> game.blackPlayer.userId,
    Fields.source -> game.source.map(_.id)
  ).noNull

  def reset(max: Option[Int]): Funit = client match {
    case c: ESClientHttp =>
      loginfo(s"Index to ${c.index.name}")
      val resetStartAt = DateTime.now
      val resetStartSeconds = nowSeconds
      writeable = false
      Thread sleep 3000
      for {
        _ <- c.putMapping
        _ <- doIndex(c, Json.obj(), max)
        _ = loginfo("[game search] Complete indexation in %ss".format(nowSeconds - resetStartSeconds))
        _ <- doIndex(c, lila.game.Query createdSince resetStartAt.minusHours(3), max)
        _ = loginfo("[game search] Complete indexation in %ss".format(nowSeconds - resetStartSeconds))
      } yield {
        writeable = true
      }
    case _ => funit
  }

  private def doIndex(client: ESClientHttp, selector: JsObject, max: Option[Int]): Funit = {
    import lila.db.api._
    import lila.game.tube.gameTube
    var nb = 0
    var nbSkipped = 0
    var started = nowMillis
    for {
      size <- $count(selector)
      batchSize = 1000
      limit = max | Int.MaxValue
      _ <- $enumerate.bulk[Option[Game]]($query(selector), batchSize, limit) { gameOptions =>
        val games = gameOptions.flatten filter storable
        val nbGames = games.size
        (GameRepo filterAnalysed games.map(_.id).toSeq flatMap { analysedIds =>
          client.storeBulk(games map { g =>
            Id(g.id) -> toDoc(g, analysedIds(g.id))
          }).logFailure("game bulk")
        }) >>- {
          nb = nb + nbGames
          nbSkipped = nbSkipped + gameOptions.size - nbGames
          val perS = (batchSize * 1000) / math.max(1, (nowMillis - started))
          started = nowMillis
          loginfo("[game search] Indexed %d of %d, skipped %d, at %d/s".format(nb + nbSkipped, size, nbSkipped, perS))
        }
      }
    } yield ()
  }
}
