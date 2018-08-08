package lidraughts.app
package templating

import controllers.routes
import play.twirl.api.Html

import lidraughts.api.Context
import lidraughts.team.Env.{ current => teamEnv }
import lidraughts.common.String.html.escapeHtml

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Html = escapeHtml(api teamName id getOrElse id)

  def teamLink(id: String, withIcon: Boolean = true): Html = Html {
    val href = routes.Team.show(id)
    val content = teamIdToName(id)
    val icon = if (withIcon) """ data-icon="f"""" else ""
    val space = if (withIcon) "&nbsp;" else ""
    s"""<a$icon href="$href">$space$content</a>"""
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
