package lila.app
package ui

import controllers.routes
import lila.i18n.I18nKeys
import lila.user.User

import play.api.mvc.Call

final class SiteMenu(trans: I18nKeys) {

  import SiteMenu._

  val play = new Elem("play", routes.Lobby.home, trans.play)
  val game = new Elem("game", routes.Game.realtime, trans.games)
  val tournament = new Elem("tournament", routes.Tournament.home, trans.tournament)
  val user = new Elem("user", routes.User.list(page = 1), trans.people)
  val team = new Elem("team", routes.Team.home(page = 1), trans.teams)
  val forum = new Elem("forum", routes.ForumCateg.index, trans.forum)
  val message = new Elem("message", routes.Message.inbox(page = 1), trans.inbox)

  private val authenticated = List(play, game, tournament, user, team, forum, message)
  private val anonymous = List(play, game, tournament, user, team, forum)

  def all(me: Option[User]) = me.isDefined.fold(authenticated, anonymous)
}

object SiteMenu {

  sealed class Elem(
    val code: String,
      val route: Call,
      val name: I18nKeys#Key) {

    def currentClass(e: Option[Elem]) = (e == Some(this)) ?? " current" 
  }
}
