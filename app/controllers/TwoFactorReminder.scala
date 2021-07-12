package controllers

import scala.concurrent.duration._

import lila.msg.MsgPreset
import lila.user.User

private[controllers] trait TwoFactorReminder { self: LilaController =>

  private val mongoCache = env.memo.mongoCacheApi
  private val userRepo   = env.user.repo
  private val msgApi     = env.msg.api

  private val cache = mongoCache[User.ID, Boolean](
    65536,
    "security:2fa",
    119 hours,
    identity
  ) { loader =>
    _.expireAfterWrite(120 hour)
      .buildAsyncFuture {
        loader { userId =>
          userRepo byId userId map2 { user =>
            if (user.totpSecret.isEmpty) {
              msgApi.postPreset(user.id, MsgPreset.enableTwoFactor)
              false
            } else true
          } map (_.getOrElse(false))
        }
      }
  }

  def sendMsgIfTwoFactorDisabled(userId: User.ID) = cache.get(userId)

}
