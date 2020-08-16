package lila.i18n

import play.api.i18n.Lang
import scala.annotation.nowarn

sealed private trait I18nQuantity

/*
 * Ported from
 * https://github.com/populov/android-i18n-plurals/tree/master/library/src/main/java/com/seppius/i18n/plurals
 *
 * Removed: boilerplate, lag, shi
 * Added: type safety, tp, io, ia
 */
private object I18nQuantity {

  case object Zero  extends I18nQuantity
  case object One   extends I18nQuantity
  case object Two   extends I18nQuantity
  case object Few   extends I18nQuantity
  case object Many  extends I18nQuantity
  case object Other extends I18nQuantity

  type Language = String
  type Selector = Count => I18nQuantity

  def apply(lang: Lang, c: Count): I18nQuantity =
    langMap.getOrElse(lang.language, selectors.default _)(c)

  private object selectors {

    def default(c: Count) =
      if (c == 1) One
      else Other

    def french(c: Count) =
      if (c < 2) One
      else Other

    def czech(c: Count) =
      if (c == 1) One
      else if (c >= 2 && c <= 4) Few
      else Other

    def balkan(c: Count) = {
      val rem100 = c % 100
      val rem10  = c % 10
      if (rem10 == 1 && rem100 != 11) One
      else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) Few
      else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) Many
      else Other
    }

    def latvian(c: Count) =
      if (c == 0) Zero
      else if (c % 10 == 1 && c % 100 != 11) One
      else Other

    def lithuanian(c: Count) = {
      val rem100 = c % 100
      val rem10  = c % 10
      if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) One
      else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) Few
      else Other
    }

    def polish(c: Count) = {
      val rem100 = c % 100
      val rem10  = c % 10
      if (c == 1) One
      else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) Few
      else Other
    }

    def romanian(c: Count) = {
      val rem100 = c % 100
      if (c == 1) One
      else if (c == 0 || (rem100 >= 1 && rem100 <= 19)) Few
      else Other
    }

    def slovenian(c: Count) = {
      val rem100 = c % 100
      if (rem100 == 1) One
      else if (rem100 == 2) Two
      else if (rem100 >= 3 && rem100 <= 4) Few
      else Other
    }

    def arabic(c: Count) = {
      val rem100 = c % 100
      if (c == 0) Zero
      else if (c == 1) One
      else if (c == 2) Two
      else if (rem100 >= 3 && rem100 <= 10) Few
      else if (rem100 >= 11 && rem100 <= 99) Many
      else Other
    }

    def macedonian(c: Count) =
      if (c % 10 == 1 && c != 11) One else Other

    def welsh(c: Count) =
      if (c == 0) Zero
      else if (c == 1) One
      else if (c == 2) Two
      else if (c == 3) Few
      else if (c == 6) Many
      else Other

    def maltese(c: Count) = {
      val rem100 = c % 100
      if (c == 1) One
      else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) Few
      else if (rem100 >= 11 && rem100 <= 19) Many
      else Other
    }

    def two(c: Count) =
      if (c == 1) One
      else if (c == 2) Two
      else Other

    def zero(c: Count) =
      if (c == 0 || c == 1) One
      else Other

    @nowarn("cat=unused") def none(c: Count) = Other
  }

  import selectors._

  private val langMap: Map[Language, Selector] = LangList.all.map {

    case (lang, _) =>
      lang.language -> (lang.language match {

        case "fr" | "ff" | "kab" => french

        case "cs" | "sk" => czech

        case "hr" | "ru" | "sr" | "uk" | "be" | "bs" | "sh" => balkan

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

        case "ak" | "am" | "bh" | "fil" | "tl" | "guw" | "hi" | "ln" | "mg" | "nso" | "ti" | "wa" =>
          selectors.zero

        case "az" | "bm" | "fa" | "ig" | "hu" | "ja" | "kde" | "kea" | "ko" | "my" | "ses" | "sg" | "to" |
            "tr" | "vi" | "wo" | "yo" | "zh" | "bo" | "dz" | "id" | "jv" | "ka" | "km" | "kn" | "ms" | "th" |
            "tp" | "io" | "ia" =>
          selectors.none

        case _ => default
      })
  }
}
