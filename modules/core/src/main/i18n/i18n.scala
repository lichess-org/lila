package lila.core
package i18n

import play.api.i18n.Lang
import play.api.mvc.RequestHeader
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
  def toDefault: Translate
trait TranslatorTxt:
  def literal(key: I18nKey, args: Seq[Any], lang: Lang): String
  def plural(key: I18nKey, count: Count, args: Seq[Any], lang: Lang): String
trait TranslatorFrag:
  def literal(key: I18nKey, args: Seq[Matchable], lang: Lang): RawFrag
  def plural(key: I18nKey, count: Count, args: Seq[Matchable], lang: Lang): RawFrag

case class Translate(translator: Translator, lang: Lang)
object Translate:
  given (using trans: Translator, lang: Lang): Translate = trans.to(lang)

trait LangList:
  val all: Map[Lang, String]
  def allLanguages: List[Language]
  def popularLanguages: List[Language]

  trait LangForm:
    def choices: List[(Language, String)]
    def mapping: play.api.data.Mapping[Language]

  def allLanguagesForm: LangForm
  def popularLanguagesForm: LangForm

trait LangPicker:
  def preferedLanguages(req: RequestHeader, prefLang: Lang): List[Language]
  def byStrOrDefault(str: Option[String]): Lang

trait JsDump:
  def keysToObject(keys: Seq[I18nKey])(using Translate): play.api.libs.json.JsObject
