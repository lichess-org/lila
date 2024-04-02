package lila.round

import lila.game.{ Game, GameRepo }

final class RecentTvGames(gameRepo: GameRepo)(using Executor):

  private val fast = scalalib.cache.ExpireSetMemo[GameId](7 minutes)
  private val slow = scalalib.cache.ExpireSetMemo[GameId](2 hours)

  def get(gameId: GameId) = fast.get(gameId) || slow.get(gameId)

  def put(game: Game) =
    gameRepo.setTv(game.id)
    (if game.speed <= chess.Speed.Bullet then fast else slow).put(game.id)
