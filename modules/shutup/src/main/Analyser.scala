package lila.shutup

import lila.common.constants.bannedYoutubeIds

object Analyser {

  def apply(raw: String) = {
    val lower = raw.toLowerCase
    TextAnalysis(
      lower,
      (
        latinBigRegex.findAllMatchIn(latinify(lower)).toList :::
          nonLatinBigRegex.findAllMatchIn(lower).toList
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

  private def latinWordsRegexes =
    Dictionary.en.map { word =>
      word + (if (word endsWith "e") "" else "e?+") + "[ds]?+"
    } ++
      Dictionary.es ++
      Dictionary.hi ++
      Dictionary.fr ++
      Dictionary.de ++
      Dictionary.tr ++
      Dictionary.it ++
      bannedYoutubeIds

  private val latinBigRegex = {
    """(?i)\b""" +
      latinWordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r

  // unicode compatible bounds
  // https://shiba1014.medium.com/regex-word-boundaries-with-unicode-207794f6e7ed
  private object bounds {
    val pre                 = """(?<=[\s,.:;"'\?!、。＋：；？！]|^)"""
    val post                = """(?=[\s,.:;"'\?!、。＋：；？！]|$)"""
    def wrap(regex: String) = pre + regex + post
  }

  private val nonLatinBigRegex = {
    """(?iu)""" + bounds.wrap(
      (Dictionary.ru ++ Dictionary.ja).mkString("(", "|", ")").replace("(", "(?:")
    )
  }.r

}
