package lila.game
package actorApi

import lila.user.User

import play.api.libs.json.JsObject
import play.api.templates.Html

case class ChangeFeatured(html: Html)
case class ChangeFeaturedGame(game: Game)
case class RenderFeaturedJs(game: Game)

case class FinishGame(game: Game, white: Option[User], black: Option[User])
case class InsertGame(game: Game)

private[game] case object NewCaptcha
