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
    """<a data-icon="f" class="%s" href="%s">&nbsp;%s</a>""".format(
      cssClass.??(" " + _),
      routes.Team.show(id),
      teamIdToName(id))
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)

  def teamNbRequests(ctx: Context) =
    (ctx.userId ?? api.nbRequests).await
}
