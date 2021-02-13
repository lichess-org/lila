package lila.game

import scala.concurrent.duration._

import lila.user.{ User, UserRepo }

final class FavoriteOpponents(
    userRepo: UserRepo,
    gameRepo: GameRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  private val userIdsCache = cacheApi[User.ID, List[(User.ID, Int)]](64, "favoriteOpponents") {
    _.expireAfterWrite(15 minutes)
      .maximumSize(4096)
      .buildAsyncFuture {
        gameRepo.favoriteOpponents(_, FavoriteOpponents.opponentLimit, FavoriteOpponents.gameLimit)
      }
  }

  def apply(userId: String): Fu[List[(User, Int)]] =
    userIdsCache get userId flatMap { opponents =>
      userRepo enabledByIds opponents.map(_._1) map {
        _ flatMap { user =>
          opponents find (_._1 == user.id) map { opponent =>
            user -> opponent._2
          }
        } sortBy (-_._2)
      }
    }
}

object FavoriteOpponents {
  private val opponentLimit = 30
  val gameLimit             = 1000
}
