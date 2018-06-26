package lila.shutup

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
      Dictionary.youtubeIds

  private val bigRegex = {
    """(?i)\b""" +
      wordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r
}
