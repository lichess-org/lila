package lila.core
package i18n

import cats.syntax.all.*
import play.api.i18n.Lang
import play.api.mvc.RequestHeader
import scalatags.Text.RawFrag
import scala.concurrent.duration.FiniteDuration
export scalalib.newtypes.{ given, * }

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

// ffs
def fixJavaLanguage(lang: Lang): Language =
  Language(lang).map(l => if l == "in" then "id" else l)

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
  def popularNoRegion: List[Lang]
  def nameByLanguage(l: Language): String
  def name(code: String): String

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

def translateDuration(duration: java.time.Duration, withMinutes: Option[Boolean] = None)(using
    Translate
): String =
  val useMinutes = withMinutes.getOrElse(duration.toDays == 0)
  List(
    (I18nKey.site.nbDays, true, duration.toDays).some,
    (I18nKey.site.nbHours, true, duration.toHours % 24).some,
    Option.when(useMinutes)(I18nKey.site.nbMinutes, false, duration.toMinutes % 60)
  ).flatten
    .dropWhile { (_, dropZero, nb) => dropZero && nb == 0 }
    .map { (key, _, nb) => key.pluralSameTxt(nb) }
    .mkString(", ")

def translateDuration(duration: FiniteDuration, withMinutes: Option[Boolean])(using Translate): String =
  translateDuration(java.time.Duration.ofSeconds(duration.toSeconds), withMinutes)
