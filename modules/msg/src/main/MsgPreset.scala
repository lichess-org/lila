package lila.msg

case class MsgPreset(name: String, text: String)

object MsgPreset {

  type Username = String

  lazy val sandbagAuto = MsgPreset(
    name = "Warning: possible sandbagging",
    text =
      """You have lost a couple games after a few moves. Please note that you MUST try to win every rated game.
Losing rated games on purpose is called "sandbagging" and is not allowed on Lichess.

Thank you for your understanding."""
  )

  lazy val boostAuto = MsgPreset(
    name = "Warning: possible boosting",
    """You have won a couple of games after a few moves. Please note that both players MUST try to win every game.
Taking advantage of opponents losing rated games on purpose is called "boosting" and is not allowed on Lichess.

Thank you for your understanding."""
  )

  lazy val sittingAuto = MsgPreset(
    name = "Warning: leaving games / stalling on time",
    text =
      """In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account."""
  )

  lazy val enableTwoFactor = MsgPreset(
    name = "Enable two-factor authentication",
    text =
      """Please enable two-factor authentication to secure your account at https://lichess.org/account/twofactor.
You received this message because your account has special responsibilities such as coach, teacher or streamer."""
  )

  def maxFollow(username: Username, max: Int) =
    MsgPreset(
      name = "Follow limit reached!",
      text = s"""Sorry, you can't follow more than $max players on Lichess.
To follow new players, you must first unfollow some on https://lichess.org/@/$username/following.

Thank you for your understanding."""
    )

  object forumDeletion {

    val presets = List(
      "public shaming",
      "disrespecting other players",
      "spamming",
      "inappropriate behavior"
    )

    def byModerator = compose("A moderator") _

    def byTeamLeader(teamSlug: String) = compose(s"A team leader of https://lichess.org/forum/$teamSlug") _

    def byBlogAuthor(authorId: String) = compose(by = s"The community blog author $authorId") _

    private def compose(by: String)(reason: String) =
      s"""$by deleted one of your posts for this reason: $reason. Please read Lichess' Forum-Etiquette: https://lichess.org/page/forum-etiquette"""
  }
}
