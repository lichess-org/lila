package lila.shutup

object Analyser {

  def apply(text: String) = TextAnalysis(
    text,
    bigRegex.findAllMatchIn(text).map(_.toString).toList
  )

  private def wordsRegexes: List[String] = Dictionary.en.map { word =>
    if (word endsWith "s") word
    else word + "s?"
  }

  private val bigRegex = {
    """(?i)\b""" +
      wordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r
}
