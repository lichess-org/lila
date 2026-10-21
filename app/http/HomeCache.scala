package lila.app
package http

import play.api.mvc.*
import scalalib.model.Language

import lila.core.i18n.toLanguage

final class HomeCache(cacheApi: lila.memo.CacheApi):

  private val supportedLangs = lila.i18n.LangList.popularLanguages.toSet

  private val cache = cacheApi.notLoading[Language, Result](supportedLangs.size, "pageCache"):
    _.expireAfterWrite(1.seconds).buildAsync()

  def apply(compute: () => Fu[Result])(using ctx: Context): Fu[Result] =
    if ctx.isAnon && supportedLangs(cacheKey) && defaultPrefs(using ctx.req) && !hasCookies(ctx.req)
    then cache.getFuture(cacheKey, _ => compute())
    else compute()

  private def cacheKey(using ctx: Context) = toLanguage(ctx.lang)

  private def defaultPrefs(using RequestHeader) =
    lila.pref.RequestPref.fromRequest == lila.pref.Pref.default

  private def hasCookies(req: RequestHeader) =
    lila.security.EmailConfirm.cookie.has(req)
