package lila.common

object LameName {

  def apply(name: String) = {
    val id = name.toLowerCase
    (lameUsernames exists id.contains) ||
      (lamePrefixes exists id.startsWith) ||
      (lameSuffixes exists id.endsWith) ||
      (uppercaseTitles exists name.startsWith)
  }

  private val titles = for {
    prefix <- List("", "w")
    char <- "ncfigl"
  } yield s"${prefix}${char}m"

  private val uppercaseTitles = titles.map(_.toUpperCase)

  private val lamePrefixes = "_" :: "-" :: (for {
    title <- titles
    sep <- List("-", "_")
  } yield s"$title$sep") ::: (0 to 9).toList map (_.toString)

  private val lameSuffixes = List("-", "_")

  private val lameUsernames = for {
    base <- List(
      "hitler",
      "fuck",
      "penis",
      "vagin",
      "anus",
      "bastard",
      "bitch",
      "shit",
      "shiz",
      "cunniling",
      "cunt",
      "kunt",
      "douche",
      "faggot",
      "jerk",
      "nigg",
      "piss",
      "poon",
      "prick",
      "pussy",
      "slut",
      "whore",
      "nazi",
      "mortez",
      "buttsex")
    replacement <- List("" -> "", "o" -> "0", "i" -> "1", "s" -> "5")
  } yield base.replace(replacement._1, replacement._2)
}
