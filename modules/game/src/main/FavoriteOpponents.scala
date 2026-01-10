package lila.game

final class FavoriteOpponents(
    userApi: lila.core.user.UserApi,
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  val opponentLimit = 30
  val gameLimit = lila.core.game.favOpponentOverGames

  private val userIdsCache = cacheApi[UserId, List[(UserId, Int)]](64, "favoriteOpponents"):
    _.expireAfterWrite(15.minutes)
      .maximumSize(4096)
      .buildAsyncFuture:
        gameRepo.favoriteOpponents(_, opponentLimit, gameLimit)

  def apply(userId: UserId): Fu[List[(User, Int)]] =
    userIdsCache.get(userId).flatMap { opponents =>
      userApi
        .enabledByIds(opponents._1F)
        .map:
          _.flatMap { user =>
            opponents.find(_._1 == user.id).map { opponent =>
              user -> opponent._2
            }
          }.sortBy(-_._2)
    }
