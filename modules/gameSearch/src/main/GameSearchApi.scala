package lila.gameSearch

import play.api.libs.json._
import scala.concurrent.duration._

import lila.game.{ Game, GameRepo }
import lila.search._

final class GameSearchApi(
    client: ESClient,
    gameRepo: GameRepo
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) extends SearchReadApi[Game, Query] {

  def search(query: Query, from: From, size: Size) =
    client.search(query, from, size) flatMap { res =>
      gameRepo gamesFromSecondary res.ids
    }

  def count(query: Query) =
    client.count(query) dmap (_.count)

  def ids(query: Query, max: Int): Fu[List[String]] =
    client.search(query, From(0), Size(max)).map(_.ids)

  def store(game: Game) =
    storable(game) ?? {
      gameRepo isAnalysed game.id flatMap { analysed =>
        lila.common.Future
          .retry(
            () => client.store(Id(game.id), toDoc(game, analysed)),
            delay = 20.seconds,
            retries = 2,
            logger.some
          )
      }
    }

  private def storable(game: Game) = game.finished || game.imported

  private def toDoc(game: Game, analysed: Boolean) =
    Json
      .obj(
        Fields.status -> (game.status match {
          case s if s.is(_.Timeout) => chess.Status.Resign
          case s if s.is(_.NoStart) => chess.Status.Resign
          case _                    => game.status
        }).id,
        Fields.turns         -> (game.turns + 1) / 2,
        Fields.rated         -> game.rated,
        Fields.perf          -> game.perfType.map(_.id),
        Fields.uids          -> game.userIds.toArray.some.filterNot(_.isEmpty),
        Fields.winner        -> game.winner.flatMap(_.userId),
        Fields.loser         -> game.loser.flatMap(_.userId),
        Fields.winnerColor   -> game.winner.fold(3)(_.color.fold(1, 2)),
        Fields.averageRating -> game.averageUsersRating,
        Fields.ai            -> game.aiLevel,
        Fields.date          -> (lila.search.Date.formatter print game.movedAt),
        Fields.duration      -> game.durationSeconds, // for realtime games only
        Fields.clockInit     -> game.clock.map(_.limitSeconds),
        Fields.clockInc      -> game.clock.map(_.incrementSeconds),
        Fields.analysed      -> analysed,
        Fields.whiteUser     -> game.whitePlayer.userId,
        Fields.blackUser     -> game.blackPlayer.userId,
        Fields.source        -> game.source.map(_.id)
      )
      .noNull
}
