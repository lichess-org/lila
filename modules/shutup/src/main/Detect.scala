package lila.shutup

object Detect {

  def find(text: String): List[String] = bigRegex.findAllMatchIn(text).map(_.toString).toList

  def count(text: String): Int = find(text).size

  def ratio(text: String): Double = {
    val nbWords = text.split("""\W+""").size
    if (nbWords == 0) 0
    else find(text).size.toDouble / nbWords
  }

  // based on https://github.com/snipe/banbuilder/blob/master/src/CensorWords.php#L97
  private val leetReplace = Map(
    'a' -> """(a|a\.|a\-|4|@|Á|á|À|Â|à|Â|â|Ä|ä|Ã|ã|Å|å|α|Δ|Λ|λ)""",
    'b' -> """(b|b\.|b\-|8|\|3|ß|Β|β)""",
    'c' -> """(c|c\.|c\-|Ç|ç|¢|€|<|\(|\{|©)""",
    'd' -> """(d|d\.|d\-|&part;|\|\)|Þ|þ|Ð|ð)""",
    'e' -> """(e|e\.|e\-|3|€|È|è|É|é|Ê|ê|∑)""",
    'f' -> """(f|f\.|f\-|ƒ)""",
    'g' -> """(g|g\.|g\-|6|9)""",
    'h' -> """(h|h\.|h\-|Η)""",
    'i' -> """(i|i\.|i\-|!|\||\]\[|]|1|∫|Ì|Í|Î|Ï|ì|í|î|ï)""",
    'j' -> """(j|j\.|j\-)""",
    'k' -> """(k|k\.|k\-|Κ|κ)""",
    'l' -> """(l|1\.|l\-|!|\||\]\[|]|£|∫|Ì|Í|Î|Ï)""",
    'm' -> """(m|m\.|m\-)""",
    'n' -> """(n|n\.|n\-|η|Ν|Π)""",
    'o' -> """(o|o\.|o\-|0|Ο|ο|Φ|¤|°|ø)""",
    'p' -> """(p|p\.|p\-|ρ|Ρ|¶|þ)""",
    'q' -> """(q|q\.|q\-)""",
    'r' -> """(r|r\.|r\-|®)""",
    's' -> """(s|s\.|s\-|5|\$|§)""",
    't' -> """(t|t\.|t\-|Τ|τ)""",
    'u' -> """(u|u\.|u\-|υ|µ)""",
    'v' -> """(v|v\.|v\-|υ|ν)""",
    'w' -> """(w|w\.|w\-|ω|ψ|Ψ)""",
    'x' -> """(x|x\.|x\-|Χ|χ)""",
    'y' -> """(y|y\.|y\-|¥|γ|ÿ|ý|Ÿ|Ý)""",
    'z' -> """(z|z\.|z\-|Ζ)""")

  private def wordsRegexes: List[String] = Dictionary.en.map { word =>
    val regex = word.map { char =>
      leetReplace.getOrElse(char, char.toString)
    }.mkString
    if (word endsWith "s") regex
    else regex + leetReplace.getOrElse('s', "s") + "?"
  }

  private val bigRegex = {
    """(?i)\b""" +
      wordsRegexes.mkString("(", "|", ")") +
      """\b"""
  }.r
}
