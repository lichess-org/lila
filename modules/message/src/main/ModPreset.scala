package lidraughts.message

case class ModPreset(subject: String, text: String)

/* From https://github.com/ornicar/lila/wiki/Canned-responses-for-moderators */
object ModPreset {

  /* First line is the message subject;
   * Other lines are the message body.
   * The message body can contain several lines.
   */
  val all = List("""

Warning: Offensive language

On Lidraughts, you *must* be nice when communicating with other players. At all times.

Lidraughts is intended to be a fun and friendly environment for everyone. Please note that repeated violation of chat policy will result in loss of chat privileges.

""", /* ---------------------------------------------------------------*/ """

Warning: Sandbagging

In your game history, you have several games where you clearly have intentionally lost the game. Attempts to artificially manipulate your own or someone else's rating are unacceptable. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

Warning: Boosting

In your game history, you have several games where the opponent clearly has intentionally lost against you. Attempts to artificially manipulate your own or someone else's rating are unacceptable. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

Warning: Excessive draw offers

Offering an excessive amount of draws in order to distract or annoy an opponent is not acceptable on Lidraughts. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

En passant

This is called "en passant" and is one of the rules of draughts. Check https://lidraughts.org/learn#/15 to learn more about it.

""", /* ---------------------------------------------------------------*/ """

Use /report

In order to report players for bad behavior, please visit https://lidraughts.org/report

""", /* ---------------------------------------------------------------*/ """

Warning: Accusations

Accusing other players of using computer assistance or otherwise cheating is not acceptable on Lidraughts. If you are confident that a player is cheating, use the report button on their profile page to report them to the moderators.

""", /* ---------------------------------------------------------------*/ """

Warning: spam is not permitted

You may post your link only once. Not once per tournament, per forum, per player, or per day: but just once. Repeated violation of this policy will result in loss of communication privileges.

""", /* ---------------------------------------------------------------*/ """

Regarding rating refunds

To receive rating refunds certain conditions must be met, in order to mitigate rating inflation. These conditions were not met in this case.
Please also remember that, over the long run, ratings tend to gravitate towards the player's real skill level.

""", /* ---------------------------------------------------------------*/ """

Warning: Username that implies you are a titled player

The username policy (https://github.com/ornicar/lila/wiki/Username-policy) for Lidraughts states that you can't have a username that implies that you have a FMJD title or the Lidraughts Master title. Actual titled players can send an email to contact@lidraughts.org with evidence that documents their identity, e.g. a scanned ID card, driving license, passport or similar. We will then verify your identity and title, and your title will be shown in front of your username and on your Lidraughts user profile. Since your username implies that you have a title, we reserve the right to close your account within two weeks, if you have not verified your title within that time.

""", /* ---------------------------------------------------------------*/ """

Account marked for computer assistance

Our cheating detection algorithms have marked your account for using computer assistance. If you want to contest the mark, please send an email to Lidraughts Contact contact@lidraughts.org. If you are a titled player, we will need a proof of your identity. It can be a picture of a document, like an ID card or a driving license.

""", /* ---------------------------------------------------------------*/ """

Streamer approval request

Thank you for creating a streamer profile! However your channel is empty, and we cannot list you as a streamer until we know what kind of content you create. Please send in another request when you have been streaming for a while and we can see what your streams are about.

""") flatMap toPreset

  private def toPreset(txt: String) =
    txt.lines.toList.map(_.trim).filter(_.nonEmpty) match {
      case subject :: body => ModPreset(subject, body mkString "\n").some
      case _ =>
        logger.warn(s"Invalid mod message preset $txt")
        none
    }

  lazy val sandbagAuto = ModPreset(
    subject = "Warning: possible sandbagging",
    text = """You have lost a couple games after a few moves. Please note that you MUST try to win every rated game.
Losing rated games on purpose is called "sandbagging", and is not allowed on Lidraughts.

Thank you for your understanding."""
  )

  lazy val sittingAuto = ModPreset(
    subject = "Warning: leaving games / stalling on time",
    text = """In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account."""
  )

  def maxFollow(username: String, max: Int) = ModPreset(
    subject = "Follow limit reached!",
    text = s"""Sorry, you can't follow more than $max players on Lidraughts.
To follow new players, you must first unfollow some on https://lidraughts.org/@/$username/following.

Thank you for your understanding."""
  )

  lazy val asJson = play.api.libs.json.Json.toJson {
    all.map { p =>
      List(p.subject, p.text)
    }
  }

  def bySubject(s: String) = all.find(_.subject == s)
}
