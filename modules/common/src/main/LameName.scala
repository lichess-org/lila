package lila.common

import scala.util.matching.Regex

object LameName {

  def username(name: String): Boolean =
    usernameRegex.find(name.replaceIf('_', "")) || lameTitlePrefix.matcher(name).lookingAt

  def tournament(name: String): Boolean = tournamentRegex find name

  private val lameTitlePrefix =
    "[Ww]?+[NCFIGl1L]M|(?i:w?+[ncfigl1])m[-_A-Z0-9]".r.pattern

  private val baseWords = List(
    "hitler",
    "fuck",
    "fvck",
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
    "resign",
    "pedo",
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
    "8814",
    "pidar",
    "pidr"
  )

  private val usernameRegex = lameWords(
    baseWords ::: List("lichess", "corona", "covid")
  )

  private val tournamentRegex = lameWords(baseWords)

  private def lameWords(list: List[String]): Regex = {
    val extras = Map(
      'a' -> "4",
      'e' -> "38",
      'g' -> "q9",
      'i' -> "l1",
      'l' -> "I1",
      'o' -> "08",
      's' -> "5",
      'u' -> "v",
      'z' -> "2"
    )

    val subs = ('a' to 'z' map { c =>
      c -> s"[$c${c.toUpper}${~extras.get(c)}]"
    }) ++ Seq('0' -> "[0O]", '1' -> "[1Il]", '8' -> "[8B]") toMap

    list
      .map {
        _.map(l => subs.getOrElse(l, l)).iterator.map(l => s"$l+").mkString
      }
      .mkString("|")
      .r
  }
}
