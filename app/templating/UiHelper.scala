package lila
package templating

import ui.Menu
import i18n.I18nKeys

import play.api.templates.Html
import play.api.mvc.RequestHeader

trait UiHelper {

  val trans: I18nKeys

  def menu(active: Option[Menu.Elem])(implicit req: RequestHeader) = Html {
    Menu.render(active)(trans)
  }
}
