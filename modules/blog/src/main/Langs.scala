package lila.blog

object Langs {
  val langs = Set("en-US", "ja-JP") // en-US has to be first
  val enIndex = 0

  def parse(langCode: String): String = {
    val langsNotEng = langs - "en-US" + "*"
    if (langsNotEng contains langCode) langCode else "en-US"
  }
}
