package lila.shutup

import lila.common.constants.bannedYoutubeIds

object Analyser {

  def apply(raw: String) = {
    val text = preprocess(raw)
    TextAnalysis(
      text,
      bigRegex.findAllMatchIn(text).map(_.toString).toList
    )
  }

  private def preprocess(text: String): String =
    text.toLowerCase map {
      case 'е' => 'e'
      case 'а' => 'a'
      case 'у' => 'y'
      case 'х' => 'x'
      case c => c
    }

  private def wordsRegexes =
    Dictionary.en.map { word =>
      word + (if (word endsWith "e") "" else "e?+") + "[ds]?+"
    } ++
      Dictionary.ru ++
      bannedYoutubeIds

  private val bigRegex = {
    """(?i)\b""" +
      wordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r
}
