package lila.message

case class ModPreset(subject: String, text: String)

/* From https://github.com/ornicar/lila/wiki/Canned-responses-for-moderators */
object ModPreset {

  /* First line is the message subject;
   * Other lines are the message body.
   * The message body can contain several lines.
   */
  val all = List("""

Warning: Leaving games / stalling on time

In your game history, you have several games where you have left the game or just let the time run out instead of playing or resigning. This can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Sandbagging

In your game history, you have several games where you clearly have intentionally lost the game. Attempts to artificially manipulate your own or someone else's rating is unacceptable. If this behavior continues to happen, we may be forced to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Boosting

In your game history, you have several games where the opponent clearly has intentionally lost against you. Attempts to artificially manipulate your own or someone else's rating is unacceptable. If this behavior continues to happen, we reserve the right to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Excessive draw offers

I would like to remind you that offering an excessive amount of draws in order to distract or annoy an opponent is not acceptable on Lichess. If this behavior continues to happen, we reserve the right to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Excessive cheat reports

I have noticed that you have reported a significant number of players for cheating. However, none or very few of these cheat reports have turned out to be accurate. Please remember that these reports have to be checked manually by Lichess moderators. Before reporting anyone for cheating, please make sure that you have requested computer analysis of the relevant game(s) and do your absolute best to avoid false reports.

""", /* ---------------------------------------------------------------*/ """

Warning: Aborting games

In your game history, you have many games where you aborted the game before play started. Repeatedly aborting games can be very annoying for your opponents. If this behavior continues to happen, we may be forced to terminate your account.

""", /* ---------------------------------------------------------------*/ """

Warning: Offensive language

On Lichess, you *must* be nice when communicating with other players. At all times.

Lichess is intended to be a fun and friendly environment for everyone. Please note that repeated violation of chat policy will result in loss of chat privileges.

""", /* ---------------------------------------------------------------*/ """

En passant

This is called "en passant" and is one of the rules of chess. Check https://en.lichess.org/learn#/15 to learn more about it.

""", /* ---------------------------------------------------------------*/ """

Use /report

In order to report players for bad behavior, please visit https://lichess.org/report

""", /* ---------------------------------------------------------------*/ """

Warning: Accusations

I would like to remind you that accusing other players of using computer assistance or otherwise cheating is not acceptable on Lichess. If you are confident that a player is cheating, use the report button on their profile page to report them to the moderators. -- Moderator on Lichess

""", /* ---------------------------------------------------------------*/ """

Warning: chat spam is not permitted

You may post your tourney/simul link in at most one public tournament, if the time control and game type is similar. Repeated violation of chat policy will result in loss of chat privileges.

""", /* ---------------------------------------------------------------*/ """

Warning: Username that implies you are a titled player

The username policy (https://github.com/ornicar/lila/wiki/Username-policy) for Lichess states that you can't have a username that implies that you have a FIDE title or the Lichess Master title. Actual titled players can send an email to support@lichess.org with evidence that documents their identity, e.g. a scanned ID card, driving license, passport or similar. We will then verify your identity and title, and your title will be shown in front of your username and on your Lichess user profile. Since your username implies that you have a title, we reserve the right to close your account within two weeks, if you have not verified your title within that time.

""", /* ---------------------------------------------------------------*/ """

Account marked for computer assistance

Our cheating detection algorithms have marked your account for using computer assistance. If you want to contest the mark please send an email to Lichess Contact contact@lichess.org. If you are a titled player we will need a proof of your identity. It can be a picture of a document, like an ID card or a driving license.

""") flatMap toPreset

  private def toPreset(txt: String) =
    txt.lines.toList.map(_.trim).filter(_.nonEmpty) match {
      case subject :: body => ModPreset(subject, body mkString "\n").some
      case _ =>
        logger.warn(s"Invalid mod message preset $txt")
        none
    }

  lazy val asJson = play.api.libs.json.Json.toJson {
    all.map { p =>
      List(p.subject, p.text)
    }
  }

  def bySubject(s: String) = all.find(_.subject == s)
}
