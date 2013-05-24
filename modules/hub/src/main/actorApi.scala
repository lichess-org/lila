package lila.hub
package actorApi

import play.api.libs.json._

case class SendTo(userId: String, message: JsObject)

object SendTo {
  def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
    SendTo(userId, Json.obj("t" -> typ, "d" -> data))
}

case class SendTos(userIds: Set[String], message: JsObject)

object SendTos {
  def apply[A: Writes](userIds: Set[String], typ: String, data: A): SendTos =
    SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
}

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
  case class SysTalk(txt: String)
  case class UnTalk(r: scala.util.matching.Regex)
  case class ReloadTournaments(html: String)
}

package timeline {
  case class ReloadTimeline(user: String)
  case class GameEntryView(rendered: String)

  sealed trait Atom
  case class Follow(u1: String, u2: String) extends Atom
  case class FollowYou(userId: String) extends Atom
  case class TeamJoin(userId: String, teamId: String) extends Atom
  case class TeamCreate(userId: String, teamId: String) extends Atom
  case class ForumPost(userId: String, categ: String, topicSlug: String, topicName: String, page: Int, post: Int) extends Atom

  object atomFormat {

    implicit val followFormat = Json.format[Follow]
    implicit val followYouFormat = Json.format[FollowYou]
    implicit val teamJoinFormat = Json.format[TeamJoin]
    implicit val teamCreateFormat = Json.format[TeamCreate]
    implicit val forumPostFormat = Json.format[ForumPost]
  }

  case class ShareEntry(user: String, data: Atom)
  case class MakeEntry(users: List[String], data: Atom)
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

package relation {
  case class GetFriends(userId: String)
  case class FriendsOf(userId: String, friends: List[String])
}
