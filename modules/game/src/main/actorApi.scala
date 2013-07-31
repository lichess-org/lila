package lila.game
package actorApi

import play.api.templates.Html

case class ChangeFeatured(html: Html)
case class ChangeFeaturedId(id: String)
case class RenderFeaturedJs(game: Game)

case class InsertGame(game: Game)

private[game] case object NewCaptcha
