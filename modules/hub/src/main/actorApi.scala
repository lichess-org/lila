package lila.hub
package actorApi

import play.api.libs.json._

case class SendTo(userId: String, message: JsObject)

object SendTo {

  def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
    SendTo(userId, Json.obj("t" -> typ, "d" -> data))
}

case class SendTos[A: Writes](userIds: Set[String], message: A)

case class Ask(msg: Any)

case class Tell(id: String, msg: Any)

case class WithUserIds(f: Iterable[String] ⇒ Unit)
case class WithSocketUserIds(id: String, f: Iterable[String] ⇒ Unit)

case object GetNbMembers
case class NbMembers(nb: Int)
case object GetUids

package captcha {
  case object AnyCaptcha
  case class GetCaptcha(id: String)
  case class ValidCaptcha(id: String, solution: String)
}

package lobby {
  case class TimelineEntry(rendered: String)
  case class SysTalk(txt: String)
  case class UnTalk(r: scala.util.matching.Regex)
  case class ReloadTournaments(html: String)
}

package game {
  case object Count
}

package message {
  case class LichessThread(to: String, subject: String, message: String)
}

package router {
  case class Abs(route: Any)
  case class Nolang(route: Any)
  case object Homepage
  case class TeamShow(id: String)
  case class Player(fullId: String)
  case class Watcher(gameId: String, color: String)
  case class Replay(gameId: String, color: String)
  case class Pgn(gameId: String)
  case class Tourney(tourId: String)
}

package forum {
  case class MakeTeam(id: String, name: String)
}

package ai {
  case object Ping
  case class Analyse(id: String, pgn: String, initialFen: Option[String])
}

package monitor {
  case object AddMove
  case object AddRequest
  case object Update
}

package round {
  case class FinishGame(gameId: String)
}

package bookmark {
  case class Toggle(gameId: String, userId: String)
  case class Remove(gameIds: List[String])
}
