package lila.common

object LameName:

  def username(name: UserName): Boolean =
    usernameRegex.find(simplify(name)) || hasTitle(name.value)

  def hasTitle(name: String): Boolean = containsTitleRegex.matches(name)

  def explain(name: UserName): Option[String] =
    if hasTitle(name.value) then "Contains a title".some
    else
      simplify(name) match
        case usernameExplainRegex(found) => s"""Lame username: "$found"""".some
        case _ => None

  private def simplify(name: UserName): String = name.value.toLowerCase.replaceIf('_', "")

  private val titlePattern = "W*(?:[NCFI1L]|I?G)"
  private val containsTitleRegex = (
    "(?i:" + titlePattern + "M[^a-z].*)|" // title at start, separated by non-letter
      + "(?:(?i:" + titlePattern + ")m[^a-z].*)|" // title at start with lowercase m, not followed by lowercase letter
      + "(?:" + titlePattern + "M.*)|" // uppercase title at start
      + "(?i:.*[^a-z]" + titlePattern + "M)|" // title at end, separated by non-letter
      + "(?i:.*[^a-z]" + titlePattern + "M[^a-z].*)|" // title in middle, surrounded by non-letters
      + "(?:.*[^A-Z]" + titlePattern + "M(?:[A-Z]?[^A-Z].*)?)" // uppercase title not preceded by uppercase letter, either at end or followed by at most one uppercase letter and then something else
  ).r

  private val baseWords = List(
    "1488",
    "8814",
    "administrator",
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
    "douche",
    "fag",
    "fart",
    "feces",
    "fuck",
    "golam",
    "hitler",
    "idiot",
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
    "putin",
    "resign",
    "retard",
    "slut",
    "suicid",
    "trump",
    "vagin",
    "wanker",
    "whore",
    "xyula",
    "xyulo",
    "xyuta"
  )

  private def usernameWords = baseWords ::: List("lichess", "corona", "covid")

  private val usernameRegex = lameWords(usernameWords).r

  private lazy val usernameExplainRegex = ("(" + lameWords(usernameWords) + ")").r.unanchored

  private def lameWords(list: List[String]): String =
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

    val subs = {
      (('a' to 'z').map { c =>
        c -> s"[$c${c.toUpper}${~extras.get(c)}]"
      }) ++ Seq('0' -> "[0O]", '1' -> "[1Il]", '8' -> "[8B]")
    }.toMap

    list
      .map:
        _.map(l => subs.getOrElse(l, l)).iterator.map(l => s"$l+").mkString
      .mkString("|")
