package lila.game
package actorApi

import play.api.libs.json.JsObject
import play.api.templates.Html

case class ChangeFeatured(html: Html)
case class ChangeFeaturedGame(game: Game)
case class RenderFeaturedJs(game: Game)

case class InsertGame(game: Game)

private[game] case object NewCaptcha
