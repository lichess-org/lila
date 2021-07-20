package lila.msg

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

import lila.memo.MongoCache
import lila.user.{ User, UserRepo }

final class TwoFactorReminder(mongoCache: MongoCache.Api, userRepo: UserRepo, api: MsgApi)(implicit
    ec: ExecutionContext
) {

  def apply(userId: User.ID) = cache get userId

  private val cache = mongoCache[User.ID, Boolean](1024, "security:2fa:reminder", 10 days, identity) {
    loader =>
      _.expireAfterWrite(11 days)
        .buildAsyncFuture {
          loader { userId =>
            userRepo hasTwoFactor userId flatMap { has =>
              (!has ?? api.postPreset(userId, MsgPreset.enableTwoFactor).void) inject has
            }
          }
        }
  }
}
