package lila.app
package templating

import controllers.routes
import lila.api.Context
import lila.relay.Relay
import lila.user.{ User, UserContext }
import lila.relay.Env.{ current => relayEnv }

import play.api.libs.json.Json
import play.twirl.api.Html

trait RelayHelper { self: I18nHelper =>

  def relayLink(relay: Relay): Html = Html {
    val url = routes.Relay.show(relay.id, relay.slug)
    s"""<a class="text" data-icon="n" href="$url">${relay.name}</a>"""
  }

  def relayIdToName(id: String) = relayEnv.cached name id getOrElse "Chess event"
}
