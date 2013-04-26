package lila.app
package templating

import play.api.templates.Html

// TODO
trait UiHelper { self: I18nHelper ⇒

  lazy val siteMenu = new lila.app.ui.SiteMenu(trans)

  // lazy val lobbyMenu = new LobbyMenu(trans)
}
