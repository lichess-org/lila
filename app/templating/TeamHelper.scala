package lila.app
package templating

import controllers.routes
import play.api.templates.Html

import lila.api.Context
import lila.team.Env.{ current => teamEnv }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.belongsTo(teamId, me.id))

  def teamIds(userId: String): List[String] = api teamIds userId

  def teamIdToName(id: String): String = api teamName id getOrElse id

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
