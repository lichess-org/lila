package lila.gameSearch

import lila.search.client.SearchClient
import lila.search.spec.Query
import lila.search.{ From, SearchReadApi, Size }

final class GameSearchApi(
    client: SearchClient,
    gameRepo: lila.core.game.GameRepo,
    userApi: lila.core.user.UserApi
)(using Executor, Scheduler)
    extends SearchReadApi[Game, Query.Game]:

  def search(query: Query.Game, from: From, size: Size): Fu[List[Game]] =
    client
      .search(query, from, size)
      .flatMap: res =>
        gameRepo.gamesFromSecondary(res.hitIds.map(GameId.apply))

  def count(query: Query.Game) =
    client.count(query).dmap(_.count)

  def validateAccounts(query: Query.Game, forMod: Boolean): Fu[Boolean] =
    fuccess(forMod) >>| userApi.containsDisabled(query.userIds).not

  def store(game: Game) =
    storable(game).so:
      gameRepo.isAnalysed(game).flatMap { analysed =>
        lila.common.LilaFuture
          .retry(
            () => client.storeGame(game.id.value, toDoc(game, analysed)),
            delay = 20.seconds,
            retries = 2,
            logger.some
          )
      }

  private def storable(game: Game) = game.finished || game.sourceIs(_.Import)

  private def status(game: Game) = game.status match
    case s if s.is(_.Timeout) => chess.Status.Resign
    case s if s.is(_.NoStart) => chess.Status.Resign
    case _                    => game.status

  private def toDoc(game: Game, analysed: Boolean): lila.search.spec.GameSource =
    lila.search.spec.GameSource(
      status = status(game).id,
      turns = (game.ply.value + 1) / 2,
      rated = game.rated,
      perf = game.perfKey.id.value,
      uids = game.userIds.some.filterNot(_.isEmpty).map(_.map(_.value)),
      winner = game.winner.flatMap(_.userId).map(_.value),
      loser = game.loser.flatMap(_.userId).map(_.value),
      winnerColor = game.winner.fold(3)(_.color.fold(1, 2)),
      averageRating = game.averageUsersRating,
      ai = game.aiLevel,
      date = lila.search.SearchDateTime.fromInstant(game.movedAt),
      duration = game.durationSeconds, // for realtime games only
      clockInit = game.clock.map(_.limitSeconds.value),
      clockInc = game.clock.map(_.incrementSeconds.value),
      analysed = analysed,
      whiteUser = game.whitePlayer.userId.map(_.value),
      blackUser = game.blackPlayer.userId.map(_.value),
      source = game.source.map(_.id)
    )
