package lila.app
package templating

import controllers.routes
import lila.simul.Simul

import play.twirl.api.Html

trait SimulHelper { self: I18nHelper =>

  def simulLink(simulId: Simul.ID): Html = Html {
    val url = routes.Simul.show(simulId)
    s"""<a class="text" data-icon="|" href="$url">Simultaneous exhibition</a>"""
  }
}
