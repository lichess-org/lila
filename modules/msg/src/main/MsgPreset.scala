package lila.msg

case class MsgPreset(name: String, text: String)

object MsgPreset {

  /* First line is the preset name;
   * Other lines are the message.
   * The message can contain several lines.
   */
  // format: off
  val all = List("""

Warning: Offensive language

On Lishogi, you *must* be nice when communicating with other players. At all times.

Lishogi is intended to be a fun and friendly environment for everyone. Please note that repeated violation of chat policy will result in loss of chat privileges.

""", /* ---------------------------------------------------------------*/ """

Warning: Sandbagging

In your game history, you have several games where you clearly have intentionally lost the game. Attempts to artificially manipulate your own or someone else's rating are unacceptable. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

Warning: Boosting

In your game history, you have several games where the opponent clearly has intentionally lost against you. Attempts to artificially manipulate your own or someone else's rating are unacceptable. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

Warning: Excessive draw offers

Offering an excessive amount of draws in order to distract or annoy an opponent is not acceptable on Lishogi. If this behavior continues to happen, your account will be terminated.

""", /* ---------------------------------------------------------------*/ """

Use /report

In order to report players for bad behavior, please visit https://lishogi.org/report

""", /* ---------------------------------------------------------------*/ """

Use /appeal

Your account has been restricted due to continuous violation of Lishogi's Terms of Service. If you would like to contest this mark or apologize for your wrongdoings, please appeal to Lishogi at https://lishogi.org/appeal and we might lift the mark.

""", /* ---------------------------------------------------------------*/ """

Warning: Accusations

Accusing other players of using computer assistance or otherwise cheating is not acceptable on Lishogi. If you are confident that a player is cheating, use the report button on their profile page to report them to the moderators.

""", /* ---------------------------------------------------------------*/ """

Warning: spam is not permitted

Spamming is not permitted on Lishogi.
Do not post anything more than once, in public chats, private chats, forums, or any other communication channel.
Please note that repeated violation of this policy will result in loss of communication privileges.

""", /* ---------------------------------------------------------------*/ """

Regarding rating refunds

To receive rating refunds certain conditions must be met, in order to mitigate rating inflation. These conditions were not met in this case.
Please also remember that, over the long run, ratings tend to gravitate towards the player's real skill level.

""", /* ---------------------------------------------------------------*/ """

Warning: Inappropriate username

Due to the fact that your username is either an inappropriate word/phrase that could be offensive to someone or is impersonating someone that you aren't, your account will be terminated within the next 24 hours.
Once this is done, you will be permitted to create a new lishogi account with a username that follows our Username policy (https://github.com/lichess-org/lila/wiki/Username-policy).

""", /* ---------------------------------------------------------------*/ """

Warning: Username or profile that implies you are a titled player

The username policy (https://github.com/lichess-org/lila/wiki/Username-policy) for Lishogi states that you can't have a username that implies that you have a JSA title or the Lishogi Master title, or impersonating a specific titled player. Actual titled players can verify by contacting us at contact@lishogi.org with evidence that documents their identity, e.g. a scanned ID card, driving license, passport or similar. We will then verify your identity and title, and your title will be shown in front of your username and on your Lishogi user profile. Since your username or profile implies that you have a title, we reserve the right to close your account within two weeks, if you have not verified your title within that time.

""", /* ---------------------------------------------------------------*/ """

Account marked for computer assistance

Our cheating detection algorithms have marked your account for using computer assistance. If you want to contest the mark or apologize for your wrongdoings, please appeal to Lishogi at https://lishogi.org/appeal and we might lift the mark. If you are a very strong player, like a high-ranking amateur or professional player, we will need a proof of your identity. It can be a picture of a document, like an ID card or a driving license. The proof of identity can also happen through Shogi Club 24, if you have an active account there. You can verify your playing strength by contacting us at contact@lishogi.org.

""", /* ---------------------------------------------------------------*/ """

Warning: leaving games / stalling on time

In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning.
This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Others

If you continue to violate Lishogi's Terms of Service (https://lishogi.org/terms-of-service) in any manner, your account will be restricted. Please refrain from doing so.

""", /* ---------------------------------------------------------------*/ """

Title Verification

Unfortunately we had to reject your title verification. You are free to make another application with the appropriate documentation.

""") flatMap toPreset
  // format: on

  private def toPreset(txt: String) =
    txt.linesIterator.toList.map(_.trim).filter(_.nonEmpty) match {
      case name :: body => MsgPreset(name, body mkString "\n").some
      case _ =>
        logger.warn(s"Invalid message preset $txt")
        none
    }

  lazy val sandbagAuto = MsgPreset(
    name = "Warning: possible sandbagging",
    text =
      """You have lost a couple games after a few moves. Please note that you MUST try to win every rated game.
Losing rated games on purpose is called "sandbagging", and is not allowed on Lishogi.

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
      text = s"""Sorry, you can't follow more than $max players on Lishogi.
To follow new players, you must first unfollow some on https://lishogi.org/@/$username/following.

Thank you for your understanding."""
    )

  lazy val asJson = play.api.libs.json.Json.toJson {
    all.map { p =>
      List(p.name, p.text)
    }
  }

  def byName(s: String) = all.find(_.name == s)
}
