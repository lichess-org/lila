package lila.msg

import lila.memo.MongoCache
import lila.core.config.BaseUrl

final class MsgByLichess(
    mongoCache: MongoCache.Api,
    userApi: lila.core.user.UserApi,
    api: MsgApi,
    baseUrl: BaseUrl
)(using
    Executor,
    lila.core.i18n.Translator
):

  object twoFactorReminder:
    def apply(userId: UserId) = cache.get(userId)
    private val cache = mongoCache[UserId, Boolean](1024, "security:2fa:reminder", 10.days, _.value):
      loader =>
        _.expireAfterWrite(3.hours)
          .maximumSize(8 * 1024)
          .buildAsyncFuture:
            loader: userId =>
              userApi
                .enabledById(userId)
                .dmap(_.filter(_.totpSecret.isEmpty))
                .flatMap:
                  case Some(user) =>
                    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
                    api.systemPost(userId, lila.core.i18n.I18nKey.tfa.setupReminder.txt()).inject(false)
                  case _ => fuccess(true)

  object emailReminder:
    def apply(userId: UserId) = cache.get(userId)
    private val emailReminderMsg = s"""No email associated with your account

  Hello, as you have an early Lichess account, no email was required when you registered.

  However this makes it easy for you to lose access to your account.
  If you forget your password, or if your password is leaked from another website, or if we decide that your password is too easy-to-guess to be secure, your account will be lost.

  Please visit $baseUrl/account/email to set your account email address. That way, you'll be able to reset your password when needed."""
    private val cache = mongoCache[UserId, Boolean](1024, "security:email:reminder", 10.days, _.value):
      loader =>
        _.expireAfterWrite(3.hours)
          .maximumSize(8 * 1024)
          .buildAsyncFuture:
            loader: userId =>
              userApi
                .enabledById(userId)
                .flatMap:
                  _.filterNot(_.hasEmail).fold(fuccess(true)): user =>
                    for _ <- api.systemPost(user.id, emailReminderMsg) yield false

  object chatTimeout:
    def apply(userId: UserId) = cache.get(userId)
    private val cache = mongoCache[UserId, Boolean](1024, "chat:timeout:msg", 1.day, _.value): loader =>
      _.expireAfterWrite(1.hour).buildAsyncFuture:
        loader: userId =>
          userApi
            .isTroll(userId)
            .not
            .flatMapz:
              api.systemPost(userId, "Chat rules violation resulted in timeout").inject(true)
