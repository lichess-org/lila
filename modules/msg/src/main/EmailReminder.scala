package lila.msg

import lila.core.config.BaseUrl
import lila.memo.MongoCache

final class EmailReminder(
    mongoCache: MongoCache.Api,
    userApi: lila.core.user.UserApi,
    api: MsgApi,
    baseUrl: BaseUrl
)(using Executor):

  def apply(userId: UserId) = cache.get(userId)

  private val emailReminderMsg = s"""No email associated with your account

Hello, as you have an early Lichess account, no email was required when you registered.

However this makes it easy for you to lose access to your account.
If you forget your password, or if your password is leaked from another website, or if we decide that your password is too easy-to-guess to be secure, your account will be lost.

Please visit $baseUrl/account/email to set your account email address. That way, you'll be able to reset your password when needed."""

  private val cache = mongoCache[UserId, Boolean](1024, "security:email:reminder", 10.days, _.value):
    loader =>
      _.expireAfterWrite(11.days)
        .maximumSize(8 * 1024)
        .buildAsyncFuture:
          loader: userId =>
            userApi
              .enabledById(userId)
              .flatMap:
                _.filterNot(_.hasEmail).fold(fuccess(true)): user =>
                  api.systemPost(userId, emailReminderMsg).inject(false)
