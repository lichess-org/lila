package lila.hub
package actorApi

import play.api.libs.json._

case class SendTo(userId: String, message: JsObject)

object SendTo {

  def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
    SendTo(userId, Json.obj("t" -> typ, "d" -> data))
}

case class SendTos[A: Writes](userIds: Set[String], message: A)

package captcha {
  case object AnyCaptcha
  case class GetCaptcha(id: String)
}

package lobby {
  case class TimelineEntry(rendered: String)
  case class Censor(username: String)
}

package message {
  case class LichessThread(to: String, subject: String, message: String) 
}

package router {
  case class Abs(route: Any)
  case class Nolang(route: Any)
  case class TeamShow(id: String)
  case class Player(fullId: String)
}

package forum {
  case class MakeTeam(id: String, name: String)
}

package ai {
  case class Analyse(pgn: String, initialFen: Option[String])
}

package monitor {
  case object AddMove
  case object AddRequest
}

package round {
  case class FinishGame(gameId: String)
}
