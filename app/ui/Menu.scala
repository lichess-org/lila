package lila
package ui

//import Global.env // OMG
import i18n.I18nKeys
import controllers.routes

import play.api.mvc.Call
import play.api.mvc.RequestHeader

object Menu {

  sealed abstract class Elem(
    val route: Call,
    val i18nKey: I18nKeys ⇒ I18nKeys#Key)

  object Play extends Elem(routes.Main.home, _.play)
  object Game extends Elem(routes.Main.home, _.games)
  object User extends Elem(routes.Main.home, _.people)
  object Forum extends Elem(routes.Main.home, _.forum)
  object Inbox extends Elem(routes.Main.home, _.inbox)

  val all = List(Play, Game, User, Forum)

  def render(active: Option[Elem])(i18n: I18nKeys)(implicit req: RequestHeader) =
    all map { elem ⇒
      """<a class="goto_nav blank_if_play%s" href="%s">%s</a>""".format(
        if (Some(elem) == active) " current" else "",
        elem.route.toString,
        elem.i18nKey(i18n)())
    } mkString
}
