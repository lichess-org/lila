package lila.security

import lila.common.constants.bannedYoutubeIds

final class Spam(spamKeywords: () => lila.common.Strings):

  def detect(text: String) =
    staticBlacklist.exists(text.contains) ||
      spamKeywords().value.exists(text.contains)

  private def referBlacklist =
    List(
      /* While links to other chess websites are welcome,
       * refer links grant the referrer money or advantages,
       * effectively inducing spam */
      "chess24.com?ref=",
      "chess24.com/?ref=",
      "chess.com/register?refId=",
      "chess.com/register?ref_id=",
      "chess.com/membership?ref_id=",
      "go.chess.com/",
      "decodechess.com/ref/",
      "aimchess.com/i/",
      "aimchess.com/try?ref=",
      "vvv.cash/?ref="
    )

  private lazy val staticBlacklist = List(
    "chess-bot.com",
    "chessbotx",
    "/auth/magic-link/login/",
    "/auth/token/",
    "/signup/confirm/",
    "/password/reset/confirm/"
  ) ::: bannedYoutubeIds ::: referBlacklist

  def replace(text: String) =
    replacements.foldLeft(text) { case (t, (regex, rep)) =>
      regex.replaceAllIn(t, rep)
    }

  /* Keep the link to the website but remove the referrer ID */
  private val replacements = List(
    """chess24.com/?\?ref=[\w-]+""".r         -> "chess24.com",
    """chess.com/register\?refId=[\w-]+""".r  -> "chess.com",
    """chess.com/register\?ref_id=[\w-]+""".r -> "chess.com",
    """go.chess.com/[\w-]+""".r               -> "chess.com",
    """vvv.cash/?\?ref=[\w-]+""".r            -> "vvv.cash",
    """aimchess.com/try\?ref=[\w-]+""".r      -> "aimchess.com",
    """aimchess.com/i/[\w-]+""".r             -> "aimchess.com",
    """\bchess-bot(\.com)?[^\s]*""".r         -> "[redacted]"
  ) ::: bannedYoutubeIds.map { id =>
    id.r -> "7orFjhLkcxA"
  }
