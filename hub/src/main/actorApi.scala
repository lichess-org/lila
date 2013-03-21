package lila.hub
package actorApi

import play.api.libs.json.JsObject

case class SendTo(userId: String, message: JsObject)
case class SendTos(userIds: Set[String], message: JsObject)
