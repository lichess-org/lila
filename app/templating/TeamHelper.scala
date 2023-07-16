package lila.app

package templating

import scalatags.Text.all.Tag
import controllers.routes

import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.hub.LightTeam.TeamName

trait TeamHelper:
  self: HasEnv with RouterHelper =>

  def isMyTeamSync(teamId: TeamId)(using ctx: Context): Boolean =
    ctx.userId.so { env.team.api.syncBelongsTo(teamId, _) }

  def teamIdToName(id: TeamId): TeamName = env.team.getTeamName(id).getOrElse(id.value)

  def teamLink(id: TeamId, withIcon: Boolean = true): Tag =
    teamLink(id, teamIdToName(id), withIcon)

  def teamLink(id: TeamId, name: Frag, withIcon: Boolean): Tag =
    a(
      href     := routes.Team.show(id),
      dataIcon := withIcon.option(lila.common.licon.Group),
      cls      := withIcon option "text"
    )(name)

  def teamForumUrl(id: TeamId) = routes.ForumCateg.show("team-" + id)

  lazy val variantTeamLinks: Map[chess.variant.Variant.LilaKey, (lila.team.Team.Mini, Frag)] =
    lila.team.Team.variants.view.mapValues { team =>
      (team, teamLink(team.id, team.name, true))
    }.toMap
