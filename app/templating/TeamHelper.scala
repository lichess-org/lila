package lila.app
package templating

import controllers.routes
import play.api.templates.Html

import lila.api.Context
import lila.team.Env.{ current => teamEnv }

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.??(me => api.belongsTo(teamId, me.id).await)

  def teamIds(userId: String): List[String] =
    api.teamIds(userId).await

  def teamIdToName(id: String): String = (api teamName id).await | id

  def teamLink(id: String, cssClass: Option[String] = None): Html = Html {
    val klass = cssClass.??(c => s""" class="$c"""")
    val href = routes.Team.show(id)
    val content = teamIdToName(id)
    s"""<a data-icon="f" $klass href="$href">&nbsp;$content</a>"""
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)

  def teamNbRequests(ctx: Context) =
    (ctx.userId ?? api.nbRequests).await
}
