package lila.socket

import play.api.libs.json.JsObject

export lila.core.lilaism.Lilaism.{ *, given }
export lila.common.extensions.*

private val logger = lila.log("socket")

// announce something to all clients
case class Announce(msg: String, date: Instant, json: JsObject)
