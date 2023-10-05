package lila.gameSearch

import play.api.libs.json.*

import lila.game.{ Game, GameRepo }
import lila.common.Json.given
import lila.search.*
import alleycats.Zero

final class GameSearchApi(
    client: ESClient,
    gameRepo: GameRepo,
    userRepo: lila.user.UserRepo
)(using Executor, Scheduler)
    extends SearchReadApi[Game, Query]:

  def search(query: Query, from: From, size: Size): Fu[List[Game]] =
    client.search(query, from, size) flatMap { res =>
      gameRepo gamesFromSecondary GameId.from(res.ids)
    }

  def count(query: Query) =
    client.count(query).dmap(_.value)

  def validateAccounts(query: Query, forMod: Boolean): Fu[Boolean] =
    fuccess(forMod) >>| !userRepo.containsDisabled(query.userIds)

  def store(game: Game) =
    storable(game).so:
      gameRepo isAnalysed game.id flatMap { analysed =>
        lila.common.LilaFuture
          .retry(
            () => client.store(game.id into Id, toDoc(game, analysed)),
            delay = 20.seconds,
            retries = 2,
            logger.some
          )
      }

  private def storable(game: Game) = game.finished || game.imported

  private def toDoc(game: Game, analysed: Boolean) =
    Json
      .obj(
        Fields.status -> game.status
          .match
            case s if s.is(_.Timeout) => chess.Status.Resign
            case s if s.is(_.NoStart) => chess.Status.Resign
            case _                    => game.status
          .id,
        Fields.turns         -> (game.ply.value + 1) / 2,
        Fields.rated         -> game.rated,
        Fields.perf          -> game.perfType.id,
        Fields.uids          -> game.userIds.some.filterNot(_.isEmpty),
        Fields.winner        -> game.winner.flatMap(_.userId),
        Fields.loser         -> game.loser.flatMap(_.userId),
        Fields.winnerColor   -> game.winner.fold(3)(_.color.fold(1, 2)),
        Fields.averageRating -> game.averageUsersRating,
        Fields.ai            -> game.aiLevel,
        Fields.date          -> lila.search.Date.formatter.print(game.movedAt),
        Fields.duration      -> game.durationSeconds, // for realtime games only
        Fields.clockInit     -> game.clock.map(_.limitSeconds),
        Fields.clockInc      -> game.clock.map(_.incrementSeconds),
        Fields.analysed      -> analysed,
        Fields.whiteUser     -> game.whitePlayer.userId,
        Fields.blackUser     -> game.blackPlayer.userId,
        Fields.source        -> game.source.map(_.id)
      )
      .noNull
