package lila.i18n

sealed trait I18nQuantity

/*
 * Ported from
 * https://github.com/populov/android-i18n-plurals/tree/master/library/src/main/java/com/seppius/i18n/plurals
 *
 * Removed: boilerplate
 * Added: type safety, tp, io, ia
 */
object I18nQuantity {

  case object Zero extends I18nQuantity
  case object One extends I18nQuantity
  case object Two extends I18nQuantity
  case object Few extends I18nQuantity
  case object Many extends I18nQuantity
  case object Other extends I18nQuantity

  def apply(lang: Lang, c: Int): I18nQuantity = lang.language match {

    case "fr" | "ff" | "kab" =>
      if (c < 2) One
      else Other

    case "cs" | "sk" =>
      if (c == 1) One
      else if (c >= 2 && c <= 4) Few
      else Other

    case "hr" | "ru" | "sr" | "uk" | "be" | "bs" | "sh" =>
      val rem100 = c % 100
      val rem10 = c % 10
      if (rem10 == 1 && rem100 != 11) One
      else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) Few
      else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) Many
      else Other

    case "lv" =>
      if (c == 0) Zero
      else if (c % 10 == 1 && c % 100 != 11) One
      else Other

    case "lt" =>
      val rem100 = c % 100;
      val rem10 = c % 10;
      if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) One
      else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) Few
      else Other

    case "pl" =>
      val rem100 = c % 100;
      val rem10 = c % 10;
      if (c == 1) One
      else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) Few
      else Other

    case "ro" | "mo" =>
      val rem100 = c % 100;
      if (c == 1) One
      else if ((c == 0 || (rem100 >= 1 && rem100 <= 19))) Few
      else Other

    case "sl" =>
      val rem100 = c % 100
      if (rem100 == 1) One
      else if (rem100 == 2) Two
      else if (rem100 >= 3 && rem100 <= 4) Few
      else Other

    case "ar" =>
      val rem100 = c % 100
      if (c == 0) Zero
      else if (c == 1) One
      else if (c == 2) Two
      else if (rem100 >= 3 && rem100 <= 10) Few
      else if (rem100 >= 11 && rem100 <= 99) Many
      else Other

    case "mk" =>
      if (c % 10 == 1 && c != 11) One else Other

    case "cy" | "br" =>
      if (c == 0) Zero
      else if (c == 1) One
      else if (c == 2) Two
      else if (c == 3) Few
      else if (c == 6) Many
      else Other

    case "mt" =>
      val rem100 = c % 100
      if (c == 1) One
      else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) Few
      else if (rem100 >= 11 && rem100 <= 19) Many
      else Other

    case "ga" | "se" | "sma" | "smi" | "smj" | "smn" | "sms" =>
      if (c == 1) One
      else if (c == 2) Two
      else Other

    case "lag" =>
      if (c == 0) Zero
      else if (c == 1) One
      else Other

    case "shi" =>
      if (c >= 0 && c <= 1) One
      else if (c >= 2 && c <= 10) Few
      else Other

    case "ak" | "am" | "bh" | "fil" | "tl" | "guw" | "hi" | "ln" | "mg" | "nso" | "ti" | "wa" =>
      if (c == 0 || c == 1) One
      else Other

    case "az" | "bm" | "fa" | "ig" | "hu" | "ja" | "kde" | "kea" | "ko" | "my" | "ses" | "sg" | "to" | "tr" | "vi" | "wo" | "yo" | "zh" | "bo" | "dz" | "id" | "jv" | "ka" | "km" | "kn" | "ms" | "th" | "tp" | "io" | "ia" =>
      Other

    case _ =>
      if (c == 1) One
      else Other
  }
}
