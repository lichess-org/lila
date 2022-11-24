package lila.game

import play.api.libs.json.*
import lila.common.Json.given

object AnonCookie:

  val name   = "rk2"
  val maxAge = 604800 // one week

  def json(pov: Pov): Option[JsObject] =
    pov.player.userId.isEmpty option Json.obj(
      "name"   -> name,
      "maxAge" -> maxAge,
      "value"  -> pov.playerId
    )
