package lila
package team

import core.CoreEnv
import controllers.routes

import play.api.templates.Html

trait TeamHelper {

  protected def env: CoreEnv

  private def cached = env.team.cached

  def teamIdToName(id: String): String = (cached name id) | id

  def teamLink(id: String, cssClass: Option[String] = None): Html = Html {
    """<a class="%s" href="%s">%s</a>""".format(
      cssClass.fold(" " + _, ""),
      routes.Team.show(id),
      teamIdToName(id))
  }

  def teamForumUrl(id: String) = routes.ForumCateg.show("team-" + id)
}
