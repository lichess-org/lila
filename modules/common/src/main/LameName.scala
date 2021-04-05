package lila.common

import scala.util.matching.Regex

object LameName {

  def username(name: String): Boolean =
    usernameRegex.find(name.replaceIf('_', "")) || lameTitlePrefix.matcher(name).lookingAt

  def tournament(name: String): Boolean = tournamentRegex find name

  private val lameTitlePrefix =
    "_*[Ww]?[NCFIG1Lncfigl][Mm]_+|_+[Ww]?[NCFIG1Lncfigl][Mm]_*|(?<![A-Z])[W]?[NCFIG1L][M]".r.pattern

  private val baseWords = List(
    "1488",
    "8814",
    "administrator",
    "anus",
    "asshole",
    "bastard",
    "biden",
    "bitch",
    "butthole",
    "buttsex",
    "cancer",
    "cheat",
    "coon",
    "cuck",
    "cunniling",
    "cunt",
    "cyka",
    "dick",
    "douche",
    "fag",
    "fart",
    "feces",
    "fuck",
    "fvck",
    "golam",
    "hitler",
    "jerk",
    "kanker",
    "kunt",
    "moderator",
    "mongool",
    "nazi",
    "nigg",
    "pedo",
    "penis",
    "pidar",
    "pidr",
    "piss",
    "poon",
    "poop",
    "poxyu",
    "pussy",
    "resign",
    "retard",
    "shit",
    "slut",
    "trump",
    "vagin",
    "wanker",
    "whore",
    "xyula",
    "xyulo",
    "xyuta"
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
