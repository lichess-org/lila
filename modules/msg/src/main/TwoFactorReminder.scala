package lila.msg

import scala.concurrent.duration.*
import scala.concurrent.ExecutionContext

import lila.memo.MongoCache
import lila.user.{ User, UserRepo }

final class TwoFactorReminder(mongoCache: MongoCache.Api, userRepo: UserRepo, api: MsgApi)(using
    ExecutionContext
):

  def apply(userId: UserId) = cache get userId

  private val cache = mongoCache[UserId, Boolean](1024, "security:2fa:reminder", 10 days, _.value) { loader =>
    _.expireAfterWrite(11 days)
      .buildAsyncFuture {
        loader { userId =>
          userRepo hasTwoFactor userId flatMap { has =>
            (!has ?? api.postPreset(userId, MsgPreset.enableTwoFactor).void) inject has
          }
        }
      }
  }
