package lila.hub
package actorApi

import play.api.libs.json.Writes

case class SendTo[A : Writes](userId: String, message: A)
case class SendTos[A : Writes](userIds: Set[String], message: A)
