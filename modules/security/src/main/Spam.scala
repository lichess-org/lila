package lila.security

object Spam {

  def detect(texts: String*) = {
    val text = texts mkString " "
    blacklist exists text.contains
  }

  private val blacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "velocitychess.com/ref/",
    "chess24.com?ref=",
    "chess.com/register?refId="
  )

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  private val replacements = List(
    """velocitychess.com/ref/\w+""".r -> "velocitychess.com",
    """chess24.com?ref=\w+""".r -> "chess24.com",
    """chess.com/register?refId=\w+""".r -> "chess.com/register"
  )
}
