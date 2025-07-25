package lila.i18n

import scala.annotation.nowarn
import play.api.i18n.Lang
import scalalib.model.Language

import lila.core.i18n.toLanguage

private enum I18nQuantity:
  case Zero, One, Two, Few, Many, Other

/*
 * Ported from
 * https://github.com/populov/android-i18n-plurals/tree/master/library/src/main/java/com/seppius/i18n/plurals
 *
 * Removed: boilerplate, lag, shi
 * Added: type safety and more languages
 */
private object I18nQuantity:

  type Selector = Count => I18nQuantity

  def fromString(s: String): Option[I18nQuantity] =
    s match
      case "zero" => Some(Zero)
      case "one" => Some(One)
      case "two" => Some(Two)
      case "few" => Some(Few)
      case "many" => Some(Many)
      case "other" => Some(Other)
      case _ => None

  def apply(lang: Lang, c: Count): I18nQuantity =
    langMap.getOrElse(toLanguage(lang), selectors.default)(c)

  private object selectors:

    def default(c: Count) =
      if c == 1 then One
      else Other

    def french(c: Count) =
      if c <= 1 then One
      else Other

    def czech(c: Count) =
      if c == 1 then One
      else if c >= 2 && c <= 4 then Few
      else Other

    def balkan(c: Count) =
      val rem100 = c % 100
      val rem10 = c % 10
      if rem10 == 1 && rem100 != 11 then One
      else if rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14) then Few
      else if rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14) then Many
      else Other

    def latvian(c: Count) =
      if c == 0 then Zero
      else if c % 10 == 1 && c % 100 != 11 then One
      else Other

    def lithuanian(c: Count) =
      val rem100 = c % 100
      val rem10 = c % 10
      if rem10 == 1 && !(rem100 >= 11 && rem100 <= 19) then One
      else if rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19) then Few
      else Other

    def polish(c: Count) =
      val rem100 = c % 100
      val rem10 = c % 10
      if c == 1 then One
      else if rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14) then Few
      else Other

    def romanian(c: Count) =
      val rem100 = c % 100
      if c == 1 then One
      else if c == 0 || (rem100 >= 1 && rem100 <= 19) then Few
      else Other

    def slovenian(c: Count) =
      val rem100 = c % 100
      if rem100 == 1 then One
      else if rem100 == 2 then Two
      else if rem100 >= 3 && rem100 <= 4 then Few
      else Other

    def arabic(c: Count) =
      val rem100 = c % 100
      if c == 0 then Zero
      else if c == 1 then One
      else if c == 2 then Two
      else if rem100 >= 3 && rem100 <= 10 then Few
      else if rem100 >= 11 && rem100 <= 99 then Many
      else Other

    def macedonian(c: Count) =
      if c % 10 == 1 && c != 11 then One else Other

    def welsh(c: Count) =
      if c == 0 then Zero
      else if c == 1 then One
      else if c == 2 then Two
      else if c == 3 then Few
      else if c == 6 then Many
      else Other

    def maltese(c: Count) =
      val rem100 = c % 100
      if c == 1 then One
      else if c == 0 || (rem100 >= 2 && rem100 <= 10) then Few
      else if rem100 >= 11 && rem100 <= 19 then Many
      else Other

    def two(c: Count) =
      if c == 1 then One
      else if c == 2 then Two
      else Other

    def none(@nowarn c: Count) = Other

  import selectors.*

  private val langMap: Map[Language, Selector] = LangList.all.map: (lang, _) =>
    toLanguage(lang) -> lang.language.match

      case "fr" | "ff" | "kab" | "co" | "ak" | "am" | "bh" | "fil" | "tl" | "guw" | "hi" | "ln" | "mg" |
          "nso" | "ti" | "wa" =>
        french

      case "cs" | "sk" => czech

      case "hr" | "ru" | "sr" | "uk" | "be" | "bs" | "sh" | "ry" => balkan

      case "lv" => latvian

      case "lt" => lithuanian

      case "pl" => polish

      case "ro" | "mo" => romanian

      case "sl" => slovenian

      case "ar" => arabic

      case "mk" => macedonian

      case "cy" | "br" => welsh

      case "mt" => maltese

      case "ga" | "se" | "sma" | "smi" | "smj" | "smn" | "sms" => two

      case "az" | "bm" | "fa" | "ig" | "hu" | "ja" | "kde" | "kea" | "ko" | "my" | "ses" | "sg" | "to" |
          "tr" | "vi" | "wo" | "yo" | "zh" | "bo" | "dz" | "id" | "jv" | "ka" | "km" | "kn" | "ms" | "th" |
          "tp" | "io" | "ia" =>
        selectors.none

      case _ => default
