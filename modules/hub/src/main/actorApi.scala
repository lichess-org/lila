package lila.hub
package actorApi

import lila.common.LightUser

import play.api.libs.json._
import play.twirl.api.Html

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

sealed abstract class Deploy(val key: String)
case object DeployPre extends Deploy("deployPre")
case object DeployPost extends Deploy("deployPost")
case class StreamsOnAir(html: String)

package map {
case class Get(id: String)
case class Tell(id: String, msg: Any)
case class TellIds(ids: Seq[String], msg: Any)
case class TellAll(msg: Any)
case class Ask(id: String, msg: Any)
}

case class WithUserIds(f: Iterable[String] => Unit)

case object GetUids
case class SocketUids(uids: Set[String])
case class HasUserId(userId: String)

package report {
case class Cheater(userId: String, text: String)
case class Clean(userId: String)
case class Check(userId: String)
case class MarkCheater(userId: String, by: String)
case class MarkTroll(userId: String, by: String)
case class Shutup(userId: String, text: String)
case class Booster(userId: String, accomplice: String)
}

package shutup {
case class RecordPublicForumMessage(userId: String, text: String)
case class RecordTeamForumMessage(userId: String, text: String)
case class RecordPrivateMessage(userId: String, toUserId: String, text: String)
case class RecordPrivateChat(chatId: String, userId: String, text: String)
case class RecordPublicChat(chatId: String, userId: String, text: String)
}

package mod {
case class MarkCheater(userId: String)
case class MarkBooster(userId: String)
}

package captcha {
case object AnyCaptcha
case class GetCaptcha(id: String)
case class ValidCaptcha(id: String, solution: String)
}

package lobby {
case class ReloadTournaments(html: String)
case class ReloadSimuls(html: String)
case object NewForumPost
}

package simul {
case object GetHostIds
case class PlayerMove(gameId: String)
}

package slack {
sealed trait Event
case class Error(msg: String) extends Event
case class Warning(msg: String) extends Event
case class Info(msg: String) extends Event
case class Victory(msg: String) extends Event
}

package timeline {
case class ReloadTimeline(user: String)

sealed abstract class Atom(val channel: String, val okForKid: Boolean)
case class Follow(u1: String, u2: String) extends Atom(s"follow", true)
case class TeamJoin(userId: String, teamId: String) extends Atom(s"teamJoin", false)
case class TeamCreate(userId: String, teamId: String) extends Atom(s"teamCreate", false)
case class ForumPost(userId: String, topicId: Option[String], topicName: String, postId: String) extends Atom(s"forum:${~topicId}", false)
case class NoteCreate(from: String, to: String) extends Atom(s"note", false)
case class TourJoin(userId: String, tourId: String, tourName: String) extends Atom(s"tournament", true)
case class QaQuestion(userId: String, id: Int, title: String) extends Atom(s"qa", true)
case class QaAnswer(userId: String, id: Int, title: String, answerId: Int) extends Atom(s"qa", true)
case class QaComment(userId: String, id: Int, title: String, commentId: String) extends Atom(s"qa", true)
case class GameEnd(playerId: String, opponent: Option[String], win: Option[Boolean], perf: String) extends Atom(s"gameEnd", true)
case class SimulCreate(userId: String, simulId: String, simulName: String) extends Atom(s"simulCreate", true)
case class SimulJoin(userId: String, simulId: String, simulName: String) extends Atom(s"simulJoin", true)
case class StudyCreate(userId: String, studyId: String, studyName: String) extends Atom(s"studyCreate", true)
case class StudyLike(userId: String, studyId: String, studyName: String) extends Atom(s"studyLike", true)

object propagation {
  sealed trait Propagation
  case class Users(users: List[String]) extends Propagation
  case class Followers(user: String) extends Propagation
  case class Friends(user: String) extends Propagation
  case class StaffFriends(user: String) extends Propagation
  case class ExceptUser(user: String) extends Propagation
}

import propagation._

case class Propagate(data: Atom, propagations: List[Propagation] = Nil) {
  def toUsers(ids: List[String]) = add(Users(ids))
  def toUser(id: String) = add(Users(List(id)))
  def toFollowersOf(id: String) = add(Followers(id))
  def toFriendsOf(id: String) = add(Friends(id))
  def toStaffFriendsOf(id: String) = add(StaffFriends(id))
  def exceptUser(id: String) = add(ExceptUser(id))
  private def add(p: Propagation) = copy(propagations = p :: propagations)
}
}

package game {
case class ChangeFeatured(id: String, msg: JsObject)
case object Count
}

package tv {
case class Select(msg: JsObject)
}

package message {
case class LichessThread(
  from: String,
  to: String,
  subject: String,
  message: String)
}

package notify {
  case class Notified(userId: String)
}

package router {
case class Abs(route: Any)
case class Nolang(route: Any)
case class TeamShow(id: String)
case class Pgn(gameId: String)
case class Puzzle(id: Int)
}

package forum {
case class MakeTeam(id: String, name: String)
}

package fishnet {
case class AutoAnalyse(gameId: String)
}

package round {
case class MoveEvent(
  gameId: String,
  fen: String,
  move: String,
  color: chess.Color,
  mobilePushable: Boolean,
  opponentUserId: Option[String],
  simulId: Option[String])
case class NbRounds(nb: Int)
case class Abort(gameId: String, byColor: String)
case class Berserk(gameId: String, userId: String)
case class IsOnGame(color: chess.Color)
sealed trait SocketEvent
object SocketEvent {
  case class OwnerJoin(gameId: String, color: chess.Color, ip: String) extends SocketEvent
  case class Stop(gameId: String) extends SocketEvent
}
case class FishnetPlay(uci: chess.format.Uci, currentFen: chess.format.FEN)
}

package evaluation {
case class AutoCheck(userId: String)
case class Refresh(userId: String)
}

package bookmark {
case class Toggle(gameId: String, userId: String)
case class Remove(gameId: String)
}

package relation {
case class ReloadOnlineFriends(userId: String)
case class GetOnlineFriends(userId: String)
case class OnlineFriends(users: List[LightUser])
case class Block(u1: String, u2: String)
case class UnBlock(u1: String, u2: String)
}

case class DonationEvent(userId: Option[String], gross: Int, net: Int, message: Option[String], progress: Int)
