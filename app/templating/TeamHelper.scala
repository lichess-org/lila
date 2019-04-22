package lila.app
package templating

import controllers.routes

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.team.Env.{ current => teamEnv }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Frag = StringFrag(api.teamName(id).getOrElse(id))

  def teamLink(id: String, withIcon: Boolean = true): Frag = a(
    href := routes.Team.show(id),
    dataIcon := withIcon.option("f")
  )(withIcon option nbsp, teamIdToName(id))

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
