package lila
package ui

import i18n.I18nKeys
import controllers.routes

import play.api.mvc.Call

final class SiteMenu(trans: I18nKeys) {

  import SiteMenu._

  val play = new Elem(routes.Lobby.home, trans.play)
  val game = new Elem(routes.Game.realtime, trans.games)
  val user = new Elem(routes.User.list(page = 1), trans.people)
  val forum = new Elem(routes.ForumCateg.index, trans.forum)
  val message = new Elem(routes.Message.inbox(page = 1), trans.inbox)

  val all = List(play, game, user, message, forum)
}

object SiteMenu {

  sealed class Elem(
      val route: Call,
      val name: I18nKeys#Key) {

    def currentClass(e: Option[Elem]) =
      if (e == Some(this)) " current" else ""
  }
}
