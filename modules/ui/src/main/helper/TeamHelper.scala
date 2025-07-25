package lila.ui

import lila.core.id.ForumCategId
import lila.core.team.LightTeam
import lila.ui.ScalatagsTemplate.{ *, given }

trait TeamHelper:
  self: AssetHelper =>

  protected def lightTeamSync: TeamId => Option[LightTeam]
  protected def syncBelongsTo: (TeamId, UserId) => Boolean

  def isMyTeamSync(teamId: TeamId)(using ctx: Context): Boolean =
    ctx.userId.exists { syncBelongsTo(teamId, _) }

  def teamIdToLight(id: TeamId): LightTeam =
    lightTeamSync(id).getOrElse(LightTeam(id, id.value, none))

  def teamLink(id: TeamId, withIcon: Boolean = true): Tag =
    teamLink(teamIdToLight(id), withIcon)

  def teamLink(team: LightTeam, withIcon: Boolean): Tag =
    a(
      href := routes.Team.show(team.id),
      dataIcon := withIcon.option(lila.ui.Icon.Group),
      cls := withIcon.option("text")
    )(team.name, teamFlair(team))

  def teamFlair(team: LightTeam): Option[Tag] = team.flair.map(teamFlair)

  def teamFlair(flair: Flair): Tag =
    img(cls := "uflair", src := flairSrc(flair))

  def teamForumUrl(id: TeamId) = routes.ForumCateg.show(ForumCategId("team-" + id))
