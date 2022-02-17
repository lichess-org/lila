package lila.app
package templating

import scalatags.Text.all.Tag
import controllers.routes
import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.team.Team.nameToId

trait TeamHelper { self: HasEnv =>

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.userId.?? { env.team.api.syncBelongsTo(teamId, _) }

  def teamIdToName(id: String): String = env.team.getTeamName(id).getOrElse(id)

  def teamLink(id: String, withIcon: Boolean = true): Tag =
    teamLink(id, teamIdToName(id), withIcon)

  def variantTeamLink(name: String, withIcon: Boolean = true): Tag =
    teamLink(nameToId(s"Lichess ${name}"), s"Lichess $name", withIcon)

  def teamLink(id: String, name: Frag, withIcon: Boolean): Tag =
    a(
      href := routes.Team.show(id),
      dataIcon := withIcon.option(""),
      cls := withIcon option "text"
    )(name)

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
