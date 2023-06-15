package lila.i18n

import play.api.i18n.Lang

object JsQuantity:
  def apply(lang: Lang): String =
    lang.language match
      case "fr" | "ff" | "kab" | "co" | "ak" | "am" | "bh" | "fil" | "tl" | "guw" | "hi" | "ln" | "mg" |
          "nso" | "ti" | "wa" => // french
        """o=>o<=1?"one":"other""""
      case "cs" | "sk" => // czech
        """o=>1==o?"one":o>=2&&o<=4?"few":"other""""
      case "hr" | "ru" | "sr" | "uk" | "be" | "bs" | "sh" | "ry" => // balkan
        """o=>{const e=o%100,t=o%10;return 1==t&&11!=e?"one":t>=2&&t<=4&&!(e>=12&&e<=14)?"few":0==t||t>=5&&t<=9||e>=11&&e<=14?"many":"other"}"""
      case "lv" => // latvian
        """o=>0==o?"zero":o%10==1&&o%100!=11?"one":"other""""
      case "lt" => // lithuanian
        """o=>{const e=o%100,t=o%10;return 1!=t||e>=11&&e<=19?t>=2&&t<=9&&!(e>=11&&e<=19)?"few":"other":"one"}"""
      case "pl" => // polish
        """o=>{const e=o%100,t=o%10;return 1==o?"one":t>=2&&t<=4&&!(e>=12&&e<=14)?"few":"other"}"""
      case "ro" | "mo" => // romanian
        """o=>{const e=o%100;return 1==o?"one":0==o||e>=1&&e<=19?"few":"other"}"""
      case "sl" => // slovenian
        """o=>{const e=o%100;return 1==e?"one":2==e?"two":e>=3&&e<=4?"few":"other"}"""
      case "ar" => // arabic
        """o=>{const e=o%100;return 0==o?"zero":1==o?"one":2==o?"two":e>=3&&e<=10?"few":e>=11&&e<=99?"many":"other"}"""
      case "mk" => // macedonian
        """o=>o%10==1&&11!=o?"one":"other""""
      case "cy" | "br" => // welsh
        """o=>0==o?"zero":1==o?"one":2==o?"two":3==o?"few":6==o?"many":"other""""
      case "mt" => // maltese
        """o=>{const e=o%100;return 1==o?"one":0==o||e>=2&&e<=10?"few":e>=11&&e<=19?"many":"other"}"""
      case "ga" | "se" | "sma" | "smi" | "smj" | "smn" | "sms" => // two
        """o=>1==o?"one":2==o?"two":"other""""
      case "az" | "bm" | "fa" | "ig" | "hu" | "ja" | "kde" | "kea" | "ko" | "my" | "ses" | "sg" | "to" |
          "tr" | "vi" | "wo" | "yo" | "zh" | "bo" | "dz" | "id" | "jv" | "ka" | "km" | "kn" | "ms" | "th" |
          "tp" | "io" | "ia" => // none
        """o=>"other""""
      case _ => // other
        """o=>1==o?"one":"other""""

/*

// $ terser --mangle --compress --ecma 2018 --safari10

export const french = c => {
  return c <= 1 ? 'one' : 'other';
};

export const czech = c => {
  if (c == 1) return 'one';
  else if (c >= 2 && c <= 4) return 'few';
  else return 'other';
};

export const balkan = c => {
  const rem100 = c % 100;
  const rem10 = c % 10;
  if (rem10 == 1 && rem100 != 11) return 'one';
  else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
  else if (rem10 == 0 || (rem10 >= 5 && rem10 <= 9) || (rem100 >= 11 && rem100 <= 14)) return 'many';
  else return 'other';
};

export const latvian = c => {
  if (c == 0) return 'zero';
  else if (c % 10 == 1 && c % 100 != 11) return 'one';
  else return 'other';
};

export const lithuanian = c => {
  const rem100 = c % 100;
  const rem10 = c % 10;
  if (rem10 == 1 && !(rem100 >= 11 && rem100 <= 19)) return 'one';
  else if (rem10 >= 2 && rem10 <= 9 && !(rem100 >= 11 && rem100 <= 19)) return 'few';
  else return 'other';
};

export const polish = c => {
  const rem100 = c % 100;
  const rem10 = c % 10;
  if (c == 1) return 'one';
  else if (rem10 >= 2 && rem10 <= 4 && !(rem100 >= 12 && rem100 <= 14)) return 'few';
  else return 'other';
};

export const romanian = c => {
  const rem100 = c % 100;
  if (c == 1) return 'one';
  else if (c == 0 || (rem100 >= 1 && rem100 <= 19)) return 'few';
  else return 'other';
};

export const slovenian = c => {
  const rem100 = c % 100;
  if (rem100 == 1) return 'one';
  else if (rem100 == 2) return 'two';
  else if (rem100 >= 3 && rem100 <= 4) return 'few';
  else return 'other';
};

export const arabic = c => {
  const rem100 = c % 100;
  if (c == 0) return 'zero';
  else if (c == 1) return 'one';
  else if (c == 2) return 'two';
  else if (rem100 >= 3 && rem100 <= 10) return 'few';
  else if (rem100 >= 11 && rem100 <= 99) return 'many';
  else return 'other';
};

export const macedonian = c => {
  return c % 10 == 1 && c != 11 ? 'one' : 'other';
};

export const welsh = c => {
  if (c == 0) return 'zero';
  else if (c == 1) return 'one';
  else if (c == 2) return 'two';
  else if (c == 3) return 'few';
  else if (c == 6) return 'many';
  else return 'other';
};

export const maltese = c => {
  const rem100 = c % 100;
  if (c == 1) return 'one';
  else if (c == 0 || (rem100 >= 2 && rem100 <= 10)) return 'few';
  else if (rem100 >= 11 && rem100 <= 19) return 'many';
  else return 'other';
};

export const two = c => {
  if (c == 1) return 'one';
  else if (c == 2) return 'two';
  else return 'other';
};

export const none = _ => 'other';

export const other = c => {
  return c == 1 ? 'one' : 'other';
};

 */
