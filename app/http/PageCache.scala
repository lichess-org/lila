package lila.app
package http

import play.api.i18n.Lang
import play.api.mvc._
import scala.concurrent.duration._

import lila.common.HTTPRequest
import lila.i18n.I18nLangPicker

final class PageCache(security: lila.security.SecurityApi, cacheApi: lila.memo.CacheApi) {

  private val cache = cacheApi.notLoading[String, Result](32, "pageCache") {
    _.expireAfterWrite(1.seconds).buildAsync()
  }

  def apply(req: RequestHeader)(compute: () => Fu[Result]): Fu[Result] =
    qualifiesWithLang(req).fold(compute()) { lang =>
      cache.getFuture(s"${HTTPRequest actionName req}($lang)", _ => compute())
    }

  private def qualifiesWithLang(req: RequestHeader): Option[String] =
    security.reqSessionId(req).isEmpty ?? {
      val lang = I18nLangPicker(req).language
      langs(lang) option lang
    }

  private val langs =
    Set("en", "ru", "tr", "de", "es", "fr", "pt", "it", "pl", "ar", "fa", "id", "nl", "nb", "sv")
}
