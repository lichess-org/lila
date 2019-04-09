package lila.app
package templating

import controllers.routes

import lila.api.Context
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.frag.escapeHtml
import lila.team.Env.{ current => teamEnv }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Frag = escapeHtml(api teamName id getOrElse id)

  def teamLink(id: String, withIcon: Boolean = true): Frag = raw {
    val href = routes.Team.show(id)
    val content = teamIdToName(id)
    val icon = if (withIcon) """ data-icon="f"""" else ""
    val space = if (withIcon) "&nbsp;" else ""
    s"""<a$icon href="$href">$space$content</a>"""
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
