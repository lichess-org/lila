package lila.game
package actorApi

import lila.user.User

import play.api.libs.json.JsObject
import play.twirl.api.Html

case class FinishGame(game: Game, white: Option[User], black: Option[User]) {

  def isVsSelf = white.isDefined && white == black
}

case class InsertGame(game: Game)

private[game] case object NewCaptcha
