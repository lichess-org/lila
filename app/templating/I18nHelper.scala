package lila.app
package templating

import play.api.i18n.Lang
import play.api.mvc.Call

import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ LangList, MessageKey, Translator }
import lila.user.UserContext

trait I18nHelper extends HasEnv with UserContext.ToLang {

  def transKey(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): Frag =
    Translator.frag.literal(key, args, lang)

  def transKeyTxt(key: MessageKey, args: Seq[Any] = Nil)(implicit lang: Lang): String =
    Translator.txt.literal(key, args, lang)

  def langName = LangList.nameByStr _

  def shortLangName(str: String) = langName(str).takeWhile(','.!=)

  def langHref(call: Call)(implicit ctx: lila.api.Context): String = langHref(call.url)
  def langHref(url: String)(implicit ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language == "en") url
    else urlWithLangQuery(url, lila.i18n.languageCode(ctx.lang))

  def langHrefJP(call: Call)(implicit ctx: lila.api.Context): String =
    langHrefJP(call.url)
  def langHrefJP(url: String)(implicit ctx: lila.api.Context): String =
    if (ctx.isAuth || ctx.lang.language != "ja") url
    else urlWithLangQuery(url, "ja")

  def urlWithLangQuery(url: String, langCode: String): String =
    s"$url${if (url.contains("?")) "&" else "?"}lang=$langCode"

}
