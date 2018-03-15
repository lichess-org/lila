package lila.security

object Spam {

  def detect(text: String) = fullBlacklist exists text.contains

  private[security] lazy val cb = "tob-ssehc".reverse

  private def referBlacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "chess24.com?ref=",
    "chess.com/register?refId="
  )

  private def tosBlacklist = List(
    cb
  )

  private lazy val fullBlacklist = referBlacklist ::: tosBlacklist

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  private[security] val tosUrl = "lichess.org/terms-of-service"

  private val protocol = """(https?://)?"""

  private val replacements = List(
    s"""chess24.com\\?ref=\\w+""" -> "chess24.com",
    s"""chess.com/register\\?refId=\\w+""" -> "chess.com",
    s"""${protocol}${cb}(\\.com)?[^\\s]*""" -> tosUrl
  ).map {
      case (regex, replacement) => regex.r -> replacement
    }
}
