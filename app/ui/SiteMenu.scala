package lila.app
package ui

import controllers.routes
import play.api.mvc.Call

import lila.i18n.{ I18nKey, I18nKeys }
import lila.user.User

final class SiteMenu(trans: I18nKeys) {

  import SiteMenu._

  val play = new Elem("play", routes.Lobby.home, trans.play)
  val game = new Elem("game", routes.Game.realtime, trans.games)
  val puzzle = new Elem("puzzle", routes.Puzzle.home, trans.training)
  val tournament = new Elem("tournament", routes.Tournament.home, trans.tournaments)
  val user = new Elem("user", routes.User.list(page = 1), trans.players)
  val team = new Elem("team", routes.Team.home(page = 1), trans.teams)
  val forum = new Elem("forum", routes.ForumCateg.index, trans.forum)
  val tv = new Elem("tv", routes.Tv.index, I18nKey.untranslated("TV"))
  val message = new Elem("message", routes.Message.inbox(page = 1), trans.inbox)

  private val authenticated = List(play, game, puzzle, tournament, user, team, forum, tv)
  private val anonymous = List(play, game, puzzle, tournament, user, team, forum, tv)

  def all(me: Option[User]) = me match {
    case Some(me)                       => authenticated
    case _                              => anonymous
  }
}

object SiteMenu {

  case class Elem(
      code: String,
      route: Call,
      name: I18nKey) {

    def currentClass(e: Option[Elem]) = (e == Some(this)) ?? " current"
  }
}
