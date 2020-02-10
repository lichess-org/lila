package lila.app
package templating

import controllers.routes

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._

trait TeamHelper { self: HasEnv =>

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => env.team.api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Frag = StringFrag(env.team.getTeamName(id).getOrElse(id))

  def teamLink(id: String, withIcon: Boolean = true) =
    a(
      href := routes.Team.show(id),
      dataIcon := withIcon.option("f"),
      cls := withIcon option "text"
    )(teamIdToName(id))

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
