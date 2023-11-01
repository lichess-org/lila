package lila.msg

import lila.user.User
import lila.hub.LightTeam
import lila.common.config.BaseUrl

case class MsgPreset(name: String, text: String)

object MsgPreset:

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

  def maxFollow(username: Username, max: Int) =
    MsgPreset(
      name = "Follow limit reached!",
      text = s"""Sorry, you can't follow more than $max players on Lichess.
To follow new players, you must first unfollow some on https://lichess.org/@/$username/following.

Thank you for your understanding."""
    )

  object forumDeletion:

    val presets = List(
      "public shaming",
      "disrespecting other players",
      "spamming",
      "inappropriate behavior",
      "incorrect subforum"
    )

    def byModerator = compose("A moderator")

    def byTeamLeader(teamSlug: String) = compose(s"A team leader of https://lichess.org/forum/$teamSlug")

    def byBlogAuthor(authorId: String) = compose(by = s"The community blog author $authorId")

    private def compose(by: String)(reason: String, forumPost: String) =
      s"""$by deleted the following of your posts for this reason: $reason. Please read Lichess' Forum-Etiquette: https://lichess.org/page/forum-etiquette
----
$forumPost
    """

  def newPermissions(by: User, team: LightTeam, perms: Iterable[String], baseUrl: BaseUrl) =
    s"""@${by.username} has changed your leader permissions in the team "${team.name}".
Your new permissions are: ${perms.mkString(", ")}.
$baseUrl/team/${team.id}"""
