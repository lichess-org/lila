package lila.shutup

import lila.common.constants.bannedYoutubeIds

object Analyser {

  def apply(raw: String) = {
    val lower = raw.toLowerCase
    TextAnalysis(
      lower,
      (
        enBigRegex.findAllMatchIn(latinify(lower)).toList :::
          ruBigRegex.findAllMatchIn(lower).toList
      ).map(_.toString)
    )
  }

  private def latinify(text: String): String =
    text map {
      case 'е' => 'e'
      case 'а' => 'a'
      case 'у' => 'y'
      case 'х' => 'x'
      case 'к' => 'k'
      case 'Н' => 'h'
      case 'о' => 'o'
      case c   => c
    }

  private def enWordsRegexes =
    Dictionary.en.map { word =>
      word + (if (word endsWith "e") "" else "e?+") + "[ds]?+"
    } ++
      bannedYoutubeIds

  private val enBigRegex = {
    """(?i)\b""" +
      enWordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r

  private val ruBigRegex = {
    """(?i)\b""" +
      Dictionary.ru.mkString("(", "|", ")") +
      """\b"""
  }.r
}
