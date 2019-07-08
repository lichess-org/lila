package lila.common

object LameName {

  def username(name: String) =
    anyName(name) || lameTitlePrefix.matcher(name).lookingAt

  def anyName(name: String) = lameWords.find(name.replaceIf('_', ""))

  def anyNameButLichessIsOk(name: String) = lameWords find {
    lichessRegex.replaceAllIn(name, "")
  }

  private val lichessRegex = "(?i)lichess".r

  private val lameTitlePrefix =
    "[Ww]?+[NCFIGl1L]M|(?i:w?+[ncfigl1])m[-_A-Z0-9]".r.pattern

  private val lameWords = {
    val extras = Map(
      'a' -> "4",
      'e' -> "38",
      'g' -> "q9",
      'i' -> "l1",
      'l' -> "I1",
      'o' -> "08",
      's' -> "5",
      'z' -> "2"
    )

    val subs = ('a' to 'z' map {
      c => c -> s"[$c${c.toUpper}${~extras.get(c)}]"
    }) ++ Seq('0' -> "[0O]", '1' -> "[1Il]", '8' -> "[8B]") toMap

    List(
      "hitler",
      "fuck",
      "fouck",
      "penis",
      "vagin",
      "anus",
      "bastard",
      "bitch",
      "shit",
      "cunniling",
      "cunt",
      "kunt",
      "douche",
      "fag",
      "golam",
      "jerk",
      "nigg",
      "coon",
      "piss",
      "poon",
      "poop",
      "pussy",
      "slut",
      "whore",
      "nazi",
      "buttsex",
      "retard",
      "pedo",
      "lichess",
      "moderator",
      "cheat",
      "administrator",
      "cock",
      "dick",
      "wanker",
      "feces",
      "fart",
      "cancer",
      "cuck",
      "butthole",
      "cyka",
      "xyuta",
      "xyulo",
      "xyula",
      "poxyu",
      "1488",
      "8814"
    ).map {
        _.map(l => subs.getOrElse(l, l)).map(_ + "+").mkString
      }.mkString("|").r
  }
}
