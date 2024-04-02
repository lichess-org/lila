package lila.msg

import lila.core.LightUser
import lila.core.config.BaseUrl
import lila.core.team.LightTeam

object MsgPreset:

  import lila.core.msg.{ MsgPreset as Msg }

  def maxFollow(username: UserName, max: Int) =
    Msg(
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

  def newPermissions(by: LightUser, team: LightTeam, perms: Iterable[String], baseUrl: BaseUrl) =
    s"""@${by.name} has changed your leader permissions in the team "${team.name}".
Your new permissions are: ${perms.mkString(", ")}.
$baseUrl/team/${team.id}"""
