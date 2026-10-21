package lila.app
package http

import play.api.mvc.*
import scalalib.model.Language

import lila.core.i18n.toLanguage
import lila.core.net.Continent
import lila.common.HTTPRequest

final class HomeCache(cacheApi: lila.memo.CacheApi, relayHome: lila.relay.RelayHomeApi):

  private val supportedLangs = lila.i18n.LangList.popularLanguages.toSet

  private val cache = cacheApi.notLoading[(Language, Option[Continent]), Result](
    supportedLangs.size * (Continent.values.size + 1),
    "pageCache"
  ):
    _.expireAfterWrite(1.seconds).buildAsync()

  def apply(compute: () => Fu[Result])(using ctx: Context): Fu[Result] =
    val lang = toLanguage(ctx.lang)
    if ctx.isAnon && supportedLangs(lang) && defaultPrefs(using ctx.req) && !hasCookies(ctx.req)
    then cache.getFuture((lang, continent), _ => compute())
    else compute()

  // the homepage relay spotlights depend on the visitor's continent,
  // but only split the cache while a continent-restricted spotlight is live
  private def continent(using ctx: Context): Option[Continent] =
    relayHome.spotlight.hasContinentRestriction.so(HTTPRequest.continent(ctx.req))

  private def defaultPrefs(using RequestHeader) =
    lila.pref.RequestPref.fromRequest == lila.pref.Pref.default

  private def hasCookies(req: RequestHeader) =
    lila.security.EmailConfirm.cookie.has(req)
