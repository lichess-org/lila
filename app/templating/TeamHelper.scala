package lila.app
package templating

import controllers.routes
import play.twirl.api.Html

import lila.api.Context
import lila.team.Env.{ current => teamEnv }
import lila.common.String.html.{ escape => escapeHtml }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.syncBelongsTo(teamId, me.id))

  def teamIdToName(id: String): Html = escapeHtml(api teamName id getOrElse id)

  def teamLink(id: String, cssClass: Option[String] = None, withIcon: Boolean = true): Html = Html {
    val klass = cssClass.??(c => s""" class="$c"""")
    val href = routes.Team.show(id)
    val content = teamIdToName(id)
    val icon = if (withIcon) """data-icon="f"""" else ""
    val space = if (withIcon) "&nbsp;" else ""
    s"""<a $icon $klass href="$href">$space$content</a>"""
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
