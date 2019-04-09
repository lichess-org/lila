package lidraughts.app
package templating

import controllers.routes

import lidraughts.api.Context
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.frag.escapeHtml
import lidraughts.team.Env.{ current => teamEnv }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Frag = escapeHtml(api teamName id getOrElse id)

  def teamLink(id: String, withIcon: Boolean = true): Frag =
    teamLink(id, teamIdToName(id), withIcon)

  def teamLink(id: String, name: Frag, withIcon: Boolean): Frag = raw {
    val href = routes.Team.show(id)
    val icon = if (withIcon) """ data-icon="f"""" else ""
    val space = if (withIcon) "&nbsp;" else ""
    s"""<a$icon href="$href">$space$name</a>"""
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
