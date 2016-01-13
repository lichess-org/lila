package lila.common

object LameName {

  def apply(n: String) = {
    val name = n.toLowerCase
    (lameUsernames exists name.contains) ||
      (lamePrefixes exists name.startsWith) ||
      (lameSuffixes exists name.endsWith)
  }

  private val lamePrefixes = "_" :: "-" :: (for {
    title <- ("wg" :: "ncfigl".toList).map(_ + "m")
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
