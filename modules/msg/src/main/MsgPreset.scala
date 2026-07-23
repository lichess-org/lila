package lila.msg

import lila.core.LightUser
import lila.core.id.ForumCategId
import lila.core.team.LightTeam

object MsgPreset:

  import lila.core.msg.MsgPreset as Msg

  private val baseUrl = "https://lichess.org"

  def maxFollow(username: UserName, max: Max) =
    Msg(
      name = "Follow limit reached!",
      text = s"""Sorry, you can't follow more than $max players on Lichess.
To follow new players, you must first unfollow some on $baseUrl/@/$username/following.

Thank you for your understanding."""
    )

  def forumRelocation(title: String, newUrl: String) =
    s"""A moderator has moved your post "$title" to a different subforum. You can find it here: $baseUrl$newUrl."""

  object forumDeletion:

    val presets = List(
      "public shaming",
      "disrespecting other players",
      "spamming",
      "inappropriate behavior",
      "incorrect subforum",
      "LLM (AI) generated content"
    )

    def byModerator = compose("A moderator")

    def byTeamLeader(forumId: ForumCategId) = compose(s"A team leader of $baseUrl/forum/$forumId")

    def byBlogAuthor(user: UserName) = compose(by = s"The community blog author $user")

    private def compose(by: String)(reason: String, forumPost: String) =
      s"""$by deleted the following of your posts for this reason: $reason. Please read Lichess' Forum-Etiquette: $baseUrl/page/forum-etiquette
----
$forumPost
    """

  def newPermissions(by: LightUser, team: LightTeam, perms: Iterable[String], teamUrl: Url) =
    s"""@${by.name} has changed your leader permissions in the team "${team.name}".
Your new permissions are: ${perms.mkString(", ")}.
${teamUrl}"""

  def payoutEligible(payoutsUrl: Url, msg: lila.core.msg.PayoutMessage) =
    import msg.*
    val deadline = finishedAt.atZone(java.time.ZoneOffset.UTC).toLocalDate.plusMonths(6)
    Msg(
      name = "Prize payout",
      text = s"""Congratulations on your finish in $tournamentName! $tournamentUrl

Lichess is offering prizes to top finishers in this tournament, and your performance means you may be eligible for a prize.

Please visit $payoutsUrl to provide the necessary information for payout. The deadline for claiming prizes is $deadline."""
    )

  def apiTokenRevoked(url: String) =
    s"""Your Lichess API token has been found on GitHub

We detected one of your API tokens in a public code repository on GitHub at the following URL:

$url

We have automatically revoked the token to protect your account.
    """
