package lila.shutup

import lila.common.constants.bannedYoutubeIds

object Analyser:

  def apply(raw: String): TextAnalysis = lila.common.Chronometer
    .sync:
      val lower = raw.take(2000).toLowerCase
      val matches = latinBigRegex.findAllMatchIn(latinify(lower)).toList :::
        ruBigRegex.findAllMatchIn(lower).toList
      TextAnalysis(lower, matches.map(_.toString))
    .mon(_.shutup.analyzer)
    .logIfSlow(100, logger)(_ => s"Slow shutup analyser ${raw take 400}")
    .result

  def isCritical(raw: String) =
    criticalRegex.find(latinify(raw.toLowerCase))

  def containsLink(raw: String) = raw.contains("http://") || raw.contains("https://")

  private val logger = lila log "security" branch "shutup"

  private def latinify(text: String): String =
    text.map:
      case 'е' => 'e'
      case 'а' => 'a'
      case 'ı' => 'i'
      case 'у' => 'y'
      case 'х' => 'x'
      case 'к' => 'k'
      case 'Н' => 'h'
      case 'о' => 'o'
      case c   => c

  private def latinWordsRegexes =
    Dictionary.en.map { word =>
      word + (if word endsWith "e" then "s?+" else "(es|s|)")
    } ++
      Dictionary.es.map { word =>
        word + (if word endsWith "e" then "" else "e?+") + "s?+"
      } ++
      Dictionary.hi ++
      Dictionary.fr.map { word =>
        word + "[sx]?+"
      } ++
      Dictionary.de.map { word =>
        word + (if word endsWith "e" then "" else "e?+") + "[nrs]?+"
      } ++
      Dictionary.tr ++
      Dictionary.it ++
      bannedYoutubeIds

  private val latinBigRegex = {
    """(?i)\b""" +
      latinWordsRegexes.mkString("(", "|", ")").replace("(", "(?:") +
      """\b"""
  }.r

  // unicode compatible bounds
  // https://shiba1014.medium.com/regex-word-boundaries-with-unicode-207794f6e7ed
  object bounds:
    val pre                 = """(?<=[\s,.:;"']|^)"""
    val post                = """(?=[\s,.:;"']|$)"""
    def wrap(regex: String) = pre + regex + post

  private val ruBigRegex = {
    """(?iu)""" + bounds.wrap:
      Dictionary.ru.mkString("(", "|", ")").replace("(", "(?:")
  }.r

  private val criticalRegex = {
    """(?i)\b""" +
      Dictionary.critical.mkString("(", "|", ")").replace("(", "(?:") +
      """\b"""
  }.r
