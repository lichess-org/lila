package lila.msg

import lila.memo.MongoCache
import lila.user.UserRepo

final class TwoFactorReminder(mongoCache: MongoCache.Api, userRepo: UserRepo, api: MsgApi)(using Executor):

  def apply(userId: UserId) = cache get userId

  private val cache = mongoCache[UserId, Boolean](1024, "security:2fa:reminder", 10 days, _.value): loader =>
    _.expireAfterWrite(11 days).buildAsyncFuture:
      loader: userId =>
        userRepo
          .withoutTwoFactor(userId)
          .flatMap:
            case Some(user) =>
              given play.api.i18n.Lang = user.realLang | lila.i18n.defaultLang
              api.systemPost(userId, lila.i18n.I18nKeys.tfa.setupReminder.txt()) inject false
            case _ => fuccess(true)
