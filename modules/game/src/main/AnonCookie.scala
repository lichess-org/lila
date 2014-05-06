package lila.game

import play.api.libs.json._

object AnonCookie {

  val name = "rk2"
  val maxAge = 604800 // one week

  def json(game: Game, color: chess.Color): Option[JsObject] =
    !game.player(color).userId.isDefined option Json.obj(
      "name" -> name,
      "maxAge" -> maxAge,
      "value" -> game.player(color).id
    )
}
