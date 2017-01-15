package lila.app
package templating

import controllers.routes
import lila.simul.Simul
import lila.simul.Env.{ current => simulEnv }

import play.twirl.api.Html

trait SimulHelper { self: I18nHelper =>

  def simulLink(simulId: Simul.ID): Html = Html {
    val url = routes.Simul.show(simulId)
    s"""<a class="text" data-icon="|" href="$url">${simulIdToName(simulId)}</a>"""
  }

  def simulIdToName(id: String) = simulEnv.cached name id getOrElse "Simul"
}
