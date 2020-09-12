package lila.i18n

import play.api.i18n.Lang

object JsQuantity {
  private def body(lang: Lang): String =
    lang.language match {
      case "fr" | "ff" | "kab" => // french
        """
return c < 2 ? 'one' : 'other';"""
      case "cs" | "sk" => // czech
        """
if (c == 1) return 'one';
else if (c >= 2 && c <= 4) return 'few';
else return 'other';
      """
      case "hr" | "ru" | "sr" | "uk" | "be" | "bs" | "sh" => // balkan
        """
var rem100 = c % 100;
var rem10 = c % 10;
if (rem10 == 1 && rem100 != 11) return 'one';
else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) return 'many';
else return 'other';
      """
      case "lv" => // latvian
        """
if (c == 0) return 'zero';
else if (c % 10 == 1 && c % 100 != 11) return 'one';
else return 'other';
      """
      case "lt" => // lithuanian
        """
var rem100 = c % 100;
var rem10 = c % 10;
if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) return 'one';
else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) return 'few';
else return 'other';
      """
      case "pl" => // polish
        """
var rem100 = c % 100;
var rem10 = c % 10;
if (c == 1) return 'one';
else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
else return 'other';
      """
      case "ro" | "mo" => // romanian
        """
var rem100 = c % 100;
if (c == 1) return 'one';
else if ((c == 0 || (rem100 >= 1 && rem100 <= 19))) return 'few';
else return 'other';
      """
      case "sl" => // slovenian
        """
var rem100 = c % 100;
if (rem100 == 1) return 'one';
else if (rem100 == 2) return 'two';
else if (rem100 >= 3 && rem100 <= 4) return 'few';
else return 'other';
      """
      case "ar" => // arabic
        """
var rem100 = c % 100;
if (c == 0) return 'zero';
else if (c == 1) return 'one';
else if (c == 2) return 'two';
else if (rem100 >= 3 && rem100 <= 10) return 'few';
else if (rem100 >= 11 && rem100 <= 99) return 'many';
else return 'other';
      """
      case "mk" => // macedonian
        """return (c % 10 == 1 && c != 11) ? 'one' : 'other';"""
      case "cy" | "br" => // welsh
        """
if (c == 0) return 'zero';
else if (c == 1) return 'one';
else if (c == 2) return 'two';
else if (c == 3) return 'few';
else if (c == 6) return 'many';
else return 'other';
      """
      case "mt" => // maltese
        """
var rem100 = c % 100;
if (c == 1) return 'one';
else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) return 'few';
else if (rem100 >= 11 && rem100 <= 19) return 'many';
else return 'other';
      """
      case "ga" | "se" | "sma" | "smi" | "smj" | "smn" | "sms" => // two
        """if (c == 1) return 'one'; else if (c == 2) return 'two'; else return 'other';"""
      case "ak" | "am" | "bh" | "fil" | "tl" | "guw" | "hi" | "ln" | "mg" | "nso" | "ti" | "wa" => // zero
        """return (c == 0 || c == 1) ? 'one' : 'other';"""
      case "az" | "bm" | "fa" | "ig" | "hu" | "ja" | "kde" | "kea" | "ko" | "my" | "ses" | "sg" | "to" |
          "tr" | "vi" | "wo" | "yo" | "zh" | "bo" | "dz" | "id" | "jv" | "ka" | "km" | "kn" | "ms" | "th" |
          "tp" | "io" | "ia" => // none
        """return 'other';"""
      case _ => // other
        """return c == 1 ? 'one' : 'other';"""
    }

  def apply(lang: Lang) = s"""function(c) {${body(lang)}}"""
}
