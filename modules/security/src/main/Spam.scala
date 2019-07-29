package lidraughts.security

import lidraughts.common.constants.bannedYoutubeIds

final class Spam(
    spamKeywords: () => lidraughts.common.Strings
) {

  def detect(text: String) = staticBlacklist.exists(text.contains) ||
    spamKeywords().value.exists(text.contains)

  private def referBlacklist = List(
    /* While links to other draughts websites are welcome,
     * refer links grant the referrer money,
     * effectively inducing spam */
    "chess24.com?ref=",
    "chess.com/register?refId=",
    "chess.com/register?ref_id="
  )

  private lazy val staticBlacklist = List("chess-bot.com") ::: bannedYoutubeIds ::: referBlacklist

  def replace(text: String) = replacements.foldLeft(text) {
    case (t, (regex, rep)) => regex.replaceAllIn(t, rep)
  }

  private val protocol = """https?://"""

  /* Keep the link to the website but remove the referrer ID */
  private val replacements = List(
    """chess24.com\?ref=\w+""".r -> "chess24.com",
    """chess.com/register\?refId=\w+""".r -> "chess.com",
    """chess.com/register\?ref_id=\w+""".r -> "chess.com",
    """\bchess-bot(\.com)?[^\s]*""".r -> "[redacted]"
  ) ::: bannedYoutubeIds.map { id =>
      id.r -> "7orFjhLkcxA"
    }
}
