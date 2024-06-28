package lila.i18n

import play.api.i18n.Lang

object LangList {

  val all = Map(
    Lang("en", "GB") -> "English",
    Lang("af", "ZA") -> "Afrikaans",
    Lang("ar", "SA") -> "العربية",
    // Lang("as", "IN")  -> "অসমীয়া",
    // Lang("az", "AZ")  -> "Azərbaycanca",
    Lang("be", "BY") -> "Беларуская",
    // Lang("bg", "BG")  -> "български език",
    // Lang("bn", "BD")  -> "বাংলা",
    // Lang("br", "FR")  -> "brezhoneg",
    // Lang("bs", "BA")  -> "bosanski",
    Lang("ca", "ES") -> "Català, valencià",
    Lang("cs", "CZ") -> "čeština",
    // Lang("cv", "CU")  -> "чӑваш чӗлхи",
    // Lang("cy", "GB")  -> "Cymraeg",
    Lang("da", "DK") -> "Dansk",
    Lang("de", "DE") -> "Deutsch",
    Lang("el", "GR") -> "Ελληνικά",
    Lang("en", "US") -> "English (US)",
    // Lang("eo", "UY")  -> "Esperanto",
    Lang("es", "ES") -> "español",
    // Lang("et", "EE")  -> "eesti keel",
    // Lang("eu", "ES")  -> "Euskara",
    Lang("fa", "IR") -> "فارسی",
    Lang("fi", "FI") -> "suomen kieli",
    // Lang("fo", "FO")  -> "føroyskt",
    Lang("fr", "FR") -> "français",
    // Lang("frp", "IT") -> "arpitan",
    // Lang("fy", "NL")  -> "Frysk",
    // Lang("ga", "IE")  -> "Gaeilge",
    // Lang("gd", "GB")  -> "Gàidhlig",
    Lang("gl", "ES") -> "Galego",
    // Lang("gu", "IN")  -> "ગુજરાતી",
    Lang("he", "IL") -> "עִבְרִית",
    Lang("hi", "IN") -> "हिन्दी, हिंदी",
    // Lang("hr", "HR")  -> "hrvatski",
    Lang("hu", "HU") -> "Magyar",
    // Lang("hy", "AM")  -> "Հայերեն",
    // Lang("ia", "IA")  -> "Interlingua",
    Lang("id", "ID") -> "Bahasa Indonesia",
    // Lang("io", "EN")  -> "Ido",
    // Lang("is", "IS")  -> "Íslenska",
    Lang("it", "IT") -> "Italiano",
    Lang("ja", "JP") -> "日本語",
    // Lang("jbo", "EN") -> "lojban",
    // Lang("jv", "ID")  -> "basa Jawa",
    // Lang("ka", "GE")  -> "ქართული",
    // Lang("kab", "DZ") -> "Taqvaylit",
    // Lang("kk", "KZ")  -> "қазақша",
    // Lang("kmr", "TR") -> "Kurdî (Kurmancî)",
    // Lang("kn", "IN")  -> "ಕನ್ನಡ",
    Lang("ko", "KR") -> "한국어",
    // Lang("ky", "KG")  -> "кыргызча",
    // Lang("la", "LA")  -> "lingua Latina",
    Lang("lt", "LT") -> "lietuvių kalba",
    // Lang("lv", "LV")  -> "latviešu valoda",
    // Lang("mg", "MG")  -> "fiteny malagasy",
    // Lang("mk", "MK")  -> "македонски јази",
    // Lang("ml", "IN")  -> "മലയാളം",
    // Lang("mn", "MN")  -> "монгол",
    // Lang("mr", "IN")  -> "मराठी",
    Lang("nb", "NO") -> "Norsk bokmål",
    // Lang("ne", "NP")  -> "नेपाली",
    Lang("nl", "NL") -> "Nederlands",
    // Lang("nn", "NO")  -> "Norsk nynorsk",
    // Lang("pi", "IN")  -> "पालि",
    Lang("pl", "PL") -> "polski",
    // Lang("ps", "AF")  -> "پښتو",
    Lang("pt", "PT") -> "Português",
    Lang("pt", "BR") -> "Português (BR)",
    Lang("ro", "RO") -> "Română",
    Lang("ru", "RU") -> "русский язык",
    // Lang("sa", "IN")  -> "संस्कृत",
    // Lang("sk", "SK")  -> "slovenčina",
    // Lang("sl", "SI")  -> "slovenščina",
    // Lang("sq", "AL")  -> "Shqip",
    Lang("sr", "SP") -> "Српски језик",
    Lang("sv", "SE") -> "svenska",
    // Lang("sw", "KE")  -> "Kiswahili",
    // Lang("ta", "IN")  -> "தமிழ்",
    // Lang("tg", "TJ")  -> "тоҷикӣ",
    Lang("th", "TH") -> "ไทย",
    // Lang("tk", "TM")  -> "Türkmençe",
    Lang("tl", "PH") -> "Tagalog",
    // Lang("tp", "TP")  -> "toki pona",
    Lang("tr", "TR") -> "Türkçe",
    Lang("uk", "UA") -> "українська",
    // Lang("ur", "PK")  -> "اُردُو",
    // Lang("uz", "UZ")  -> "oʻzbekcha",
    Lang("vi", "VN") -> "Tiếng Việt",
    // Lang("yo", "NG")  -> "Yorùbá",
    Lang("zh", "CN") -> "中文",
    Lang("zh", "TW") -> "繁體中文"
    // Lang("zu", "ZA")  -> "isiZulu"
  )

  lazy val popular: List[Lang] = {
    // 01/06/2023 based on db.user4.aggregate({$sortByCount:'$lang'}).toArray()
    // GB should be third, but let's have en together
    val langs =
      List(
        "en-US",
        "en-GB",
        "ja-JP",
        "ru-RU",
        "es-ES",
        "pt-BR",
        "fr-FR",
        "de-DE",
        "zh-CN",
        "vi-VN",
        "pl-PL",
        "it-IT",
        "tr-TR",
        "zh-TW",
        "nl-NL",
        "ko-KR",
        "cs-CZ",
        "pt-PT",
        "sv-SE",
        "hu-HU",
        "uk-UA",
        "fi-FI",
        "da-DK",
        "th-TH",
        "ar-SA",
        "no-NO",
        "ro-RO",
        "ca-ES",
        "el-GR",
        "he-IL",
        "sr-SP",
        "id-ID",
        "nb-NO",
        "be-BY",
        "af-ZA",
        "hi-IN",
        "tk-TM",
        "tl-PH",
        "fa-IR",
        "lt-LT"
      )
        .flatMap(Lang.get)
        .zipWithIndex
        .toMap
    all.keys.toList.sortBy(l => langs.getOrElse(l, Int.MaxValue))
  }

  // based on https://crowdin.com/project/lishogi
  // at least 80%, en not included
  private val mostlyTranslated =
    Set(
      "be-BY",
      "zh-CN",
      "cs-CZ",
      "fr-FR",
      "de-DE",
      // "hi-IN",
      "it-IT",
      "ja-JP",
      "pl-PL",
      "pt-BR",
      "ru-RU",
      "es-ES",
      "tr-TR",
      "vi-VN"
    )

  sealed trait AlternativeLangs
  case object EnglishJapanese                      extends AlternativeLangs
  case object All                                  extends AlternativeLangs
  case class Custom(langPath: Map[String, String]) extends AlternativeLangs

  // no english, sorted by popularity
  lazy val alternativeHrefLangCodes: List[String] =
    popular.withFilter(l => mostlyTranslated.contains(l.code)).map(languageCode).take(15)

  def name(lang: Lang): String   = all.getOrElse(lang, lang.code)
  def name(code: String): String = Lang.get(code).fold(code)(name)

  def nameByStr(str: String): String = I18nLangPicker.byStr(str).fold(str)(name)

  lazy val choices: List[(String, String)] = all.toList
    .map { case (l, name) =>
      l.code -> name
    }
    .sortBy(_._1)
}
