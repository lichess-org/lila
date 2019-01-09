package lila.shutup

import lila.common.constants.bannedYoutubeIds

object Analyser {

  def apply(text: String) = TextAnalysis(
    text,
    bigRegex.findAllMatchIn(text).map(_.toString).toList
  )

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
