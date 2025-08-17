package lila.i18n

import play.api.i18n.Lang
import scalalib.model.{ Language, LangTag }

import lila.core.i18n.{ toLanguage, fixJavaLanguage }

object LangList extends lila.core.i18n.LangList:

  val all: Map[Lang, String] = Map(
    Lang("en", "GB") -> "English",
    Lang("af", "ZA") -> "Afrikaans",
    Lang("an", "ES") -> "Aragonés",
    Lang("ar", "SA") -> "العربية",
    Lang("ast", "ES") -> "Asturianu",
    Lang("av", "RU") -> "авар мацӀ",
    Lang("az", "AZ") -> "Azərbaycanca",
    Lang("be", "BY") -> "Беларуская",
    Lang("bg", "BG") -> "български език",
    Lang("bn", "BD") -> "বাংলা",
    Lang("br", "FR") -> "Brezhoneg",
    Lang("bs", "BA") -> "Bosanski",
    Lang("ca", "ES") -> "Català, valencià",
    Lang("ckb", "IR") -> "کوردی سۆرانی",
    Lang("co", "FR") -> "Corsu",
    Lang("cs", "CZ") -> "Čeština",
    Lang("cv", "CU") -> "чӑваш чӗлхи",
    Lang("cy", "GB") -> "Cymraeg",
    Lang("da", "DK") -> "Dansk",
    Lang("de", "DE") -> "Deutsch",
    Lang("el", "GR") -> "Ελληνικά",
    Lang("en", "US") -> "English (US)",
    Lang("eo", "AA") -> "Esperanto",
    Lang("es", "ES") -> "Español",
    Lang("et", "EE") -> "Eesti keel",
    Lang("eu", "ES") -> "Euskara",
    Lang("fa", "IR") -> "فارسی",
    Lang("fi", "FI") -> "Suomen kieli",
    Lang("fo", "FO") -> "Føroyskt",
    Lang("fr", "FR") -> "Français",
    Lang("fy", "NL") -> "Frysk",
    Lang("ga", "IE") -> "Gaeilge",
    Lang("gd", "GB") -> "Gàidhlig",
    Lang("gl", "ES") -> "Galego",
    Lang("gsw", "CH") -> "Schwizerdütsch",
    Lang("gu", "IN") -> "ગુજરાતી",
    Lang("he", "IL") -> "עִבְרִית",
    Lang("hi", "IN") -> "हिन्दी, हिंदी",
    Lang("hr", "HR") -> "Hrvatski",
    Lang("hu", "HU") -> "Magyar",
    Lang("hy", "AM") -> "Հայերեն",
    Lang("ia", "AA") -> "Interlingua",
    Lang("id", "ID") -> "Bahasa Indonesia",
    Lang("is", "IS") -> "Íslenska",
    Lang("it", "IT") -> "Italiano",
    Lang("ja", "JP") -> "日本語",
    Lang("jbo", "AA") -> "Lojban",
    Lang("ka", "GE") -> "ქართული",
    Lang("kab", "DZ") -> "Taqvaylit",
    Lang("kk", "KZ") -> "қазақша",
    Lang("kmr", "TR") -> "Kurdî (Kurmancî)",
    Lang("kn", "IN") -> "ಕನ್ನಡ",
    Lang("ko", "KR") -> "한국어",
    Lang("la", "VA") -> "Lingua Latina",
    Lang("lb", "LU") -> "Lëtzebuergesch",
    Lang("lt", "LT") -> "Lietuvių kalba",
    Lang("lv", "LV") -> "Latviešu valoda",
    Lang("mg", "MG") -> "Fiteny malagasy",
    Lang("mk", "MK") -> "македонски јази",
    Lang("ml", "IN") -> "മലയാളം",
    Lang("mn", "MN") -> "монгол",
    Lang("mr", "IN") -> "मराठी",
    Lang("nb", "NO") -> "Norsk bokmål",
    Lang("ne", "NP") -> "नेपाली",
    Lang("nl", "NL") -> "Nederlands",
    Lang("nn", "NO") -> "Norsk nynorsk",
    Lang("pi", "IN") -> "पालि",
    Lang("pl", "PL") -> "Polski",
    Lang("ps", "AF") -> "پښتو",
    Lang("pt", "PT") -> "Português",
    Lang("pt", "BR") -> "Português (BR)",
    Lang("ro", "RO") -> "Română",
    Lang("ru", "RU") -> "русский язык",
    Lang("ry", "UA") -> "Русинська бисїда",
    Lang("sk", "SK") -> "Slovenčina",
    Lang("sl", "SI") -> "Slovenščina",
    Lang("so", "SO") -> "Af Soomaali",
    Lang("sq", "AL") -> "Shqip",
    Lang("sr", "SP") -> "Српски језик",
    Lang("sv", "SE") -> "Svenska",
    Lang("sw", "KE") -> "Kiswahili",
    Lang("ta", "IN") -> "தமிழ்",
    Lang("th", "TH") -> "ไทย",
    Lang("tk", "TM") -> "Türkmençe",
    Lang("tl", "PH") -> "Tagalog",
    Lang("tok", "AA") -> "Toki pona",
    Lang("tr", "TR") -> "Türkçe",
    Lang("uk", "UA") -> "українська",
    Lang("ur", "PK") -> "اُردُو",
    Lang("uz", "UZ") -> "oʻzbekcha",
    Lang("vi", "VN") -> "Tiếng Việt",
    Lang("zh", "CN") -> "中文",
    Lang("zh", "TW") -> "繁體中文",
    Lang("zu", "ZA") -> "isiZulu"
  )

  val defaultRegions = Map[String, Lang](
    "de" -> Lang("de", "DE"),
    "en" -> Lang("en", "US"),
    "pt" -> Lang("pt", "BR"),
    "zh" -> Lang("zh", "CN")
  )

  def removeRegion(lang: Lang): Lang =
    defaultRegions.get(lang.language) | lang

  lazy val popular: List[Lang] =
    // 26/04/2020 based on db.user4.aggregate({$sortByCount:'$lang'}).toArray()
    val langs: Map[Lang, Int] =
      "en-US en-GB ru-RU es-ES tr-TR fr-FR de-DE pt-BR it-IT pl-PL ar-SA fa-IR nl-NL id-ID nb-NO el-GR sv-SE uk-UA cs-CZ vi-VN sr-SP hr-HR hu-HU pt-PT he-IL fi-FI ca-ES da-DK ro-RO zh-CN bg-BG sk-SK ko-KR az-AZ ja-JP sl-SI lt-LT ka-GE mn-MN bs-BA hy-AM zh-TW lv-LV et-EE th-TH gl-ES sq-AL eu-ES hi-IN mk-MK uz-UZ be-BY ms-MY bn-BD is-IS af-ZA nn-NO ta-IN as-IN la-LA kk-KZ tl-PH mr-IN eo-UY gu-IN ky-KG kn-IN ml-IN cy-GB no-NO fo-FO zu-ZA jv-ID ga-IE ur-PK ur-IN te-IN sw-KE am-ET ia-IA sa-IN si-LK ps-AF mg-MG kmr-TR ne-NP tk-TM fy-NL pa-PK br-FR tt-RU cv-CU tg-TJ tp-TP yo-NG frp-IT pi-IN my-MM pa-IN kab-DZ io-EN gd-GB jbo-EN io-IO ckb-IR ceb-PH an-ES"
        .split(' ')
        .flatMap(Lang.get)
        .zipWithIndex
        .toMap
    all.keys.toList.sortBy(l => langs.getOrElse(l, Int.MaxValue))

  lazy val popularNoRegion: List[Lang] = popular.collect:
    case l if defaultRegions.get(l.language).forall(_ == l) => l

  lazy val allLanguages: List[Language] = popularNoRegion.map(fixJavaLanguage)
  lazy val popularLanguages: List[Language] = allLanguages.take(20)
  lazy val popularAlternateLanguages: List[Language] = allLanguages.drop(1).take(20)

  def name(lang: Lang): String = all.getOrElse(lang, lang.code)
  def name(tag: LangTag): String = tag.toLang.fold(tag.value)(name)

  def nameByStr(str: String): String = LangPicker.byStr(str).fold(str)(name)
  def nameByLanguage(l: Language): String = nameByStr(l.value)

  lazy val languageChoices: List[(Language, String)] = all.view
    .map: (l, name) =>
      toLanguage(l) -> name
    .toList
    .distinctBy(_._1)
    .sortBy(_._1.value)

  lazy val popularLanguageChoices: List[(Language, String)] =
    popularNoRegion.flatMap: lang =>
      all.get(lang).map(toLanguage(lang) -> _)

  lazy val allChoices: List[(String, String)] = all.view
    .map: (l, name) =>
      l.code -> name
    .toList
    .sortBy(_._1)

  lazy val allLanguagesForm = new LangForm:
    val choices = languageChoices
    val mapping = languageMapping(choices)

  lazy val popularLanguagesForm = new LangForm:
    val choices = popularLanguageChoices
    val mapping = languageMapping(choices)

  private def languageMapping(choices: List[(Language, String)]): play.api.data.Mapping[Language] =
    play.api.data.Forms.text.verifying(l => choices.exists(_._1.value == l)).transform(Language(_), _.value)
