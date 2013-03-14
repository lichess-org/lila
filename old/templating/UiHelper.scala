package lila.app
package templating

import http.Context
import ui._
import i18n.I18nKeys

import play.api.templates.Html

trait UiHelper {

  val trans: I18nKeys

  lazy val siteMenu = new SiteMenu(trans)

  lazy val lobbyMenu = new LobbyMenu(trans)
}
