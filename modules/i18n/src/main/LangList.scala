package lila.i18n

import play.api.i18n.Lang
import scalalib.model.{ Language, LangTag }

import lila.core.i18n.{ toLanguage, fixJavaLanguage }

object LangList extends lila.core.i18n.LangList:

  val all: Map[Lang, String] = Map(
    Lang("af", "ZA") -> "Afrikaans",
    Lang("so", "SO") -> "Af Soomaali",
    Lang("an", "ES") -> "Aragonés",
    Lang("ast", "ES") -> "Asturianu",
    Lang("az", "AZ") -> "Azərbaycanca",
    Lang("id", "ID") -> "Bahasa Indonesia",
    Lang("bs", "BA") -> "Bosanski",
    Lang("br", "FR") -> "Brezhoneg",
    Lang("ca", "ES") -> "Català, valencià",
    Lang("cs", "CZ") -> "Čeština",
    Lang("co", "FR") -> "Corsu",
    Lang("cy", "GB") -> "Cymraeg",
    Lang("da", "DK") -> "Dansk",
    Lang("de", "DE") -> "Deutsch",
    Lang("et", "EE") -> "Eesti keel",
    Lang("en", "GB") -> "English",
    Lang("en", "US") -> "English (US)",
    Lang("es", "ES") -> "Español",
    Lang("eo", "AA") -> "Esperanto",
    Lang("eu", "ES") -> "Euskara",
    Lang("mg", "MG") -> "Fiteny malagasy",
    Lang("fo", "FO") -> "Føroyskt",
    Lang("fr", "FR") -> "Français",
    Lang("fy", "NL") -> "Frysk",
    Lang("ga", "IE") -> "Gaeilge",
    Lang("gd", "GB") -> "Gàidhlig",
    Lang("gl", "ES") -> "Galego",
    Lang("hr", "HR") -> "Hrvatski",
    Lang("ia", "AA") -> "Interlingua",
    Lang("zu", "ZA") -> "IsiZulu",
    Lang("is", "IS") -> "Íslenska",
    Lang("it", "IT") -> "Italiano",
    Lang("sw", "KE") -> "Kiswahili",
    Lang("kmr", "TR") -> "Kurdî (Kurmancî)",
    Lang("lv", "LV") -> "Latviešu valoda",
    Lang("lb", "LU") -> "Lëtzebuergesch",
    Lang("lt", "LT") -> "Lietuvių kalba",
    Lang("la", "VA") -> "Lingua Latina",
    Lang("jbo", "AA") -> "Lojban",
    Lang("hu", "HU") -> "Magyar",
    Lang("nl", "NL") -> "Nederlands",
    Lang("nb", "NO") -> "Norsk bokmål",
    Lang("nn", "NO") -> "Norsk nynorsk",
    Lang("uz", "UZ") -> "Oʻzbekcha",
    Lang("pl", "PL") -> "Polski",
    Lang("pt", "PT") -> "Português",
    Lang("pt", "BR") -> "Português (BR)",
    Lang("ro", "RO") -> "Română",
    Lang("gsw", "CH") -> "Schwizerdütsch",
    Lang("sq", "AL") -> "Shqip",
    Lang("sk", "SK") -> "Slovenčina",
    Lang("sl", "SI") -> "Slovenščina",
    Lang("fi", "FI") -> "Suomen kieli",
    Lang("sv", "SE") -> "Svenska",
    Lang("tl", "PH") -> "Tagalog",
    Lang("kab", "DZ") -> "Taqvaylit",
    Lang("vi", "VN") -> "Tiếng Việt",
    Lang("tok", "AA") -> "Toki pona",
    Lang("tr", "TR") -> "Türkçe",
    Lang("tk", "TM") -> "Türkmençe",
    Lang("el", "GR") -> "Ελληνικά",
    Lang("av", "RU") -> "Авар мацӀ",
    Lang("be", "BY") -> "Беларуская",
    Lang("bg", "BG") -> "Български език",
    Lang("kk", "KZ") -> "Қазақша",
    Lang("mk", "MK") -> "Македонски јази",
    Lang("mn", "MN") -> "Монгол",
    Lang("ry", "UA") -> "Русинська бисїда",
    Lang("ru", "RU") -> "Русский язык",
    Lang("sr", "SP") -> "Српски језик",
    Lang("uk", "UA") -> "Українська",
    Lang("cv", "CU") -> "Чӑваш чӗлхи",
    Lang("ka", "GE") -> "ქართული",
    Lang("hy", "AM") -> "Հայերեն",
    Lang("he", "IL") -> "עִבְרִית",
    Lang("ur", "PK") -> "اُردُو",
    Lang("ar", "SA") -> "العربية",
    Lang("ps", "AF") -> "پښتو",
    Lang("fa", "IR") -> "فارسی",
    Lang("ckb", "IR") -> "کوردی سۆرانی",
    Lang("ne", "NP") -> "नेपाली",
    Lang("pi", "IN") -> "पालि",
    Lang("mr", "IN") -> "मराठी",
    Lang("hi", "IN") -> "हिन्दी, हिंदी",
    Lang("bn", "BD") -> "বাংলা",
    Lang("gu", "IN") -> "ગુજરાતી",
    Lang("ta", "IN") -> "தமிழ்",
    Lang("kn", "IN") -> "ಕನ್ನಡ",
    Lang("ml", "IN") -> "മലയാളം",
    Lang("th", "TH") -> "ไทย",
    Lang("zh", "CN") -> "中文",
    Lang("ja", "JP") -> "日本語",
    Lang("zh", "TW") -> "繁體中文",
    Lang("ko", "KR") -> "한국어"
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
