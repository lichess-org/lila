package lila.msg

import lila.memo.MongoCache
import lila.core.config.BaseUrl
import lila.core.i18n.I18nKey.msg as trans
import lila.core.msg.SystemMsg

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
                    val msg =
                      SystemMsg.standard(userId, lila.core.i18n.I18nKey.tfa.setupReminder.txt())
                    for _ <- api.systemPost(msg)
                    yield false
                  case _ => fuccess(true)

  def lichobileDeprecationMessage(user: lila.core.user.User) =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    val text =
      s"""${trans.lichobileNewAppAvailable.txt()}\n\n${trans.lichobileNewAppDownload.txt(s"$baseUrl/app")}"""
    api.systemPost(SystemMsg.mustRead(user.id, text))

  object chatTimeout:
    def apply(userId: UserId) = cache.get(userId)
    private val text = s"""Chat rules violation resulted in timeout

Please review the chat rules on ${lila.core.chat.etiquetteUrl}."""
    private val cache = mongoCache[UserId, Boolean](1024, "chat:timeout:msg", 1.day, _.value): loader =>
      _.expireAfterWrite(1.hour).buildAsyncFuture:
        loader: userId =>
          userApi
            .isTroll(userId)
            .not
            .flatMapz:
              api.systemPost(SystemMsg.mustRead(userId, text)).inject(true)
