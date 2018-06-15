package lila.security

object Spam {

  def detect(text: String) = fullBlacklist exists text.contains

  private def referBlacklist = List(
    /* While links to other chess websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "chess24.com?ref=",
    "chess.com/register?refId="
  )

  private val youtubeIds = List(
    "7UpltimWY_E"
  )

  private lazy val fullBlacklist = List(
    "chess-bot.com"
  ) ::: youtubeIds ::: referBlacklist

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  private val protocol = """https?://"""

  private val replacements = List(
    """chess24.com\?ref=\w+""".r -> "chess24.com",
    """chess.com/register\?refId=\w+""".r -> "chess.com",
    """\bchess-bot(\.com)?[^\s]*""".r -> "[redacted]"
  ) ::: youtubeIds.map { id =>
      id.r -> "7orFjhLkcxA"
    }
}
