package lila.msg

case class MsgPreset(name: String, text: String)

object MsgPreset {

  lazy val sandbagAuto = MsgPreset(
    name = "Warning: possible sandbagging",
    text =
      """You have lost a couple games after a few moves. Please note that you MUST try to win every rated game.
Losing rated games on purpose is called "sandbagging", and is not allowed on Lichess.

Thank you for your understanding."""
  )

  lazy val sittingAuto = MsgPreset(
    name = "Warning: leaving games / stalling on time",
    text =
      """In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account."""
  )

  def maxFollow(username: String, max: Int) =
    MsgPreset(
      name = "Follow limit reached!",
      text = s"""Sorry, you can't follow more than $max players on Lichess.
To follow new players, you must first unfollow some on https://lichess.org/@/$username/following.

Thank you for your understanding."""
    )
}
