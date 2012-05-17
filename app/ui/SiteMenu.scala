package lila
package ui

import i18n.I18nKeys
import controllers.routes

import play.api.mvc.Call

final class SiteMenu(trans: I18nKeys) {

  import SiteMenu._

  val play = new Elem(routes.Lobby.home, trans.play)
  val game = new Elem(routes.Game.realtime, trans.games)
  val user = new Elem(routes.Lobby.home, trans.people)
  val forum = new Elem(routes.Lobby.home, trans.forum)
  val inbox = new Elem(routes.Lobby.home, trans.inbox)

  val all = List(play, game, user, forum)
}

object SiteMenu {

  sealed class Elem(
      val route: Call,
      val name: I18nKeys#Key) {

    def currentClass(e: Option[Elem]) =
      if (e == Some(this)) " current" else ""
  }
}
