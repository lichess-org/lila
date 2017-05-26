package lila.i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import play.twirl.api.Html

object Translator {

  def html(key: String, args: List[Any], lang: Lang): Html =
    Html(str(key, args, lang))

  def str(key: String, args: List[Any], lang: Lang): String =
    translate(key, args, lang) getOrElse key

  def transTo(key: String, args: Seq[Any], lang: Lang): String =
    translate(key, args, lang) getOrElse key

  def rawTranslation(lang: Lang)(key: String): Option[String] =
    I18nDb.all get lang flatMap (_ get key)

  private def translate(key: String, args: Seq[Any], lang: Lang): Option[String] =
    I18nDb.all.get(lang) orElse I18nDb.all.get(defaultLang) flatMap (_ get key) flatMap {
      formatTranslation(key, _, args)
    }

  private def formatTranslation(key: String, pattern: String, args: Seq[Any]) = try {
    Some(if (args.isEmpty) pattern else pattern.format(args: _*))
  }
  catch {
    case e: Exception =>
      logger.warn(s"Failed to translate $key -> $pattern ($args)", e)
      None
  }
}
