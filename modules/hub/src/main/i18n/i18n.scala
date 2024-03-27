package lila.hub
package i18n

import play.api.i18n.Lang
import scalatags.Text.RawFrag

/* play.api.i18n.Lang is composed of language and country.
 * Let's make new types for those so we don't mix them.
 */
opaque type Language = String
object Language extends OpaqueString[Language]:
  def apply(lang: Lang): Language = lang.language

opaque type Country = String
object Country extends OpaqueString[Country]:
  def apply(lang: Lang): Country = lang.country

val defaultLanguage: Language = "en"
val enLang: Lang              = Lang("en", "GB")
val defaultLang: Lang         = enLang

type Count = Long

trait Translator:
  val txt: TranslatorTxt
  val frag: TranslatorFrag
  def to(lang: Lang): Translate
trait TranslatorTxt:
  def literal(key: I18nKey, args: Seq[Any], lang: Lang): String
  def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String
trait TranslatorFrag:
  def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag
  def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag

case class Translate(translator: Translator, lang: Lang)
object Translate:
  // given Conversion[Translate, Translator]                = _.translator
  // given Conversion[Translate, Lang] = _.lang
  // given (using translate: Translate): Translator         = translate.translator
  // given (using translate: Translate): Lang               = translate.lang
  given (using trans: Translator, lang: Lang): Translate = trans.to(lang)
