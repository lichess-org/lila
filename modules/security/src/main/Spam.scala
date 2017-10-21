package lila.security

object Spam {

  def detect(texts: String*) = {
    val text = texts mkString " "
    fullBlacklist exists text.contains
  }

  val cb = "tob-ssehc".reverse

  private val referBlacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "velocitychess.com/ref/",
    "chess24.com?ref=",
    "chess.com/register?refId=",
    /* links to cheats */
    cb
  )

  private val fullBlacklist = referBlacklist

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  val tosUrl = "lichess.org/terms-of-service"

  val protocol = """(https?://)?"""

  private val replacements = List(
    s"""${protocol}velocitychess.com/ref/\\w+""" -> "velocitychess.com",
    s"""${protocol}chess24.com?ref=\\w+""" -> "chess24.com",
    s"""${protocol}chess.com/register?refId=\\w+""" -> "chess.com",
    s"""${protocol}${cb}(\\.com)?[^\\s]*""" -> tosUrl
  ).map {
      case (regex, replacement) => regex.r -> replacement
    }
}
