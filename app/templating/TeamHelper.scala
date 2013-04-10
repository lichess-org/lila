package lila.app
package templating

import lila.team.Env.{ current ⇒ teamEnv }
import lila.user.Context
import controllers.routes

import play.api.templates.Html

trait TeamHelper {

  private def api = teamEnv.api

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ctx.me.zmap(me ⇒ api.belongsTo(teamId, me.id).await)

  def teamIds(userId: String): List[String] = 
    api.teamIds(userId).await

  def teamIdToName(id: String): String = (api teamName id).await | id

  def teamLink(id: String, cssClass: Option[String] = None): Html = Html {
    """<a class="%s" href="%s">%s</a>""".format(
      cssClass.zmap(" " + _),
      routes.Team.show(id),
      teamIdToName(id))
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)

  def teamNbRequests(ctx: Context) = 
    (ctx.userId zmap api.nbRequests).await
}
