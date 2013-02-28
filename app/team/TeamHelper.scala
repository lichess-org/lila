package lila.app
package team

import core.CoreEnv
import http.Context
import controllers.routes

import play.api.templates.Html

trait TeamHelper {

  protected def env: CoreEnv

  private def cached = env.team.cached

  def myTeam(teamId: String)(implicit ctx: Context): Boolean =
    ~ctx.me.map(me ⇒ env.team.api.belongsTo(teamId, me.id))

  def teamIds(userId: String): List[String] = env.team.cached.teamIds(userId)

  def teamIdToName(id: String): String = (cached name id) | id

  def teamLink(id: String, cssClass: Option[String] = None): Html = Html {
    """<a class="%s" href="%s">%s</a>""".format(
      ~cssClass.map(" " + _),
      routes.Team.show(id),
      teamIdToName(id))
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)

  def teamNbRequests(ctx: Context) = ~ctx.me.map(cached.nbRequests)
}
