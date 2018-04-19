package lila.hub
package actorApi

import org.joda.time.DateTime
import play.api.libs.json._
import chess.format.Uci

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

package streamer {
  case class StreamsOnAir(html: String)
  case class StreamStart(userId: String)
}

package map {
  case class Get(id: String)
  case class Tell(id: String, msg: Any)
  case class TellIds(ids: Seq[String], msg: Any)
  case class TellAll(msg: Any)
  case class Ask(id: String, msg: Any)
  case class Exists(id: String)
}

case class WithUserIds(f: Iterable[String] => Unit)

case class HasUserId(userId: String)

package report {
  case class Cheater(userId: String, text: String)
  case class Shutup(userId: String, text: String)
  case class Booster(winnerId: String, loserId: String)
  case class Created(userId: String, reason: String, reporterId: String)
  case class Processed(userId: String, reason: String)
}

package security {
  case class GarbageCollect(userId: String, ipBan: Boolean)
  case class CloseAccount(userId: String)
}

package shutup {
  case class RecordPublicForumMessage(userId: String, text: String)
  case class RecordTeamForumMessage(userId: String, text: String)
  case class RecordPrivateMessage(userId: String, toUserId: String, text: String)
  case class RecordPrivateChat(chatId: String, userId: String, text: String)
  case class RecordPublicChat(userId: String, text: String, source: PublicSource)

  sealed trait PublicSource
  object PublicSource {
    case class Tournament(id: String) extends PublicSource
    case class Simul(id: String) extends PublicSource
    case class Study(id: String) extends PublicSource
    case class Watcher(gameId: String) extends PublicSource
  }
}

package mod {
  case class MarkCheater(userId: String, value: Boolean)
  case class MarkBooster(userId: String)
  case class ChatTimeout(mod: String, user: String, reason: String)
  case class Shadowban(user: String, value: Boolean)
  case class KickFromRankings(userId: String)
}

package playban {
  case class Playban(userId: String, mins: Int)
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

  sealed abstract class Atom(val channel: String, val okForKid: Boolean) {
    def userIds: List[String]
  }
  case class Follow(u1: String, u2: String) extends Atom("follow", true) {
    def userIds = List(u1, u2)
  }
  case class TeamJoin(userId: String, teamId: String) extends Atom("teamJoin", false) {
    def userIds = List(userId)
  }
  case class TeamCreate(userId: String, teamId: String) extends Atom("teamCreate", false) {
    def userIds = List(userId)
  }
  case class ForumPost(userId: String, topicId: Option[String], topicName: String, postId: String) extends Atom(s"forum:${~topicId}", false) {
    def userIds = List(userId)
  }
  case class NoteCreate(from: String, to: String) extends Atom("note", false) {
    def userIds = List(from, to)
  }
  case class TourJoin(userId: String, tourId: String, tourName: String) extends Atom("tournament", true) {
    def userIds = List(userId)
  }
  case class QaQuestion(userId: String, id: Int, title: String) extends Atom("qa", true) {
    def userIds = List(userId)
  }
  case class QaAnswer(userId: String, id: Int, title: String, answerId: Int) extends Atom("qa", true) {
    def userIds = List(userId)
  }
  case class QaComment(userId: String, id: Int, title: String, commentId: String) extends Atom("qa", true) {
    def userIds = List(userId)
  }
  case class GameEnd(playerId: String, opponent: Option[String], win: Option[Boolean], perf: String) extends Atom("gameEnd", true) {
    def userIds = opponent.toList
  }
  case class SimulCreate(userId: String, simulId: String, simulName: String) extends Atom("simulCreate", true) {
    def userIds = List(userId)
  }
  case class SimulJoin(userId: String, simulId: String, simulName: String) extends Atom("simulJoin", true) {
    def userIds = List(userId)
  }
  case class StudyCreate(userId: String, studyId: String, studyName: String) extends Atom("studyCreate", true) {
    def userIds = List(userId)
  }
  case class StudyLike(userId: String, studyId: String, studyName: String) extends Atom("studyLike", true) {
    def userIds = List(userId)
  }
  case class PlanStart(userId: String) extends Atom("planStart", true) {
    def userIds = List(userId)
  }
  case class BlogPost(id: String, slug: String, title: String) extends Atom("blogPost", true) {
    def userIds = Nil
  }
  case class StreamStart(id: String, name: String) extends Atom("streamStart", true) {
    def userIds = List(id)
  }

  object propagation {
    sealed trait Propagation
    case class Users(users: List[String]) extends Propagation
    case class Followers(user: String) extends Propagation
    case class Friends(user: String) extends Propagation
    case class StaffFriends(user: String) extends Propagation
    case class ExceptUser(user: String) extends Propagation
    case class ModsOnly(value: Boolean) extends Propagation
  }

  import propagation._

  case class Propagate(data: Atom, propagations: List[Propagation] = Nil) {
    def toUsers(ids: List[String]) = add(Users(ids))
    def toUser(id: String) = add(Users(List(id)))
    def toFollowersOf(id: String) = add(Followers(id))
    def toFriendsOf(id: String) = add(Friends(id))
    def toStaffFriendsOf(id: String) = add(StaffFriends(id))
    def exceptUser(id: String) = add(ExceptUser(id))
    def modsOnly(value: Boolean) = add(ModsOnly(value))
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

package notify {
  case class Notified(userId: String)
}

package team {
  case class CreateTeam(id: String, name: String, userId: String)
  case class JoinTeam(id: String, userId: String)
}

package fishnet {
  case class AutoAnalyse(gameId: String)
  case class NewKey(userId: String, key: String)
  case class StudyChapterRequest(
      studyId: String,
      chapterId: String,
      initialFen: Option[chess.format.FEN],
      variant: chess.variant.Variant,
      moves: List[Uci],
      userId: Option[String]
  )
}

package user {
  case class Note(from: String, to: String, text: String, mod: Boolean)
}

package round {
  case class MoveEvent(
      gameId: String,
      fen: String,
      move: String
  )
  case class CorresMoveEvent(
      move: MoveEvent,
      playerUserId: Option[String],
      mobilePushable: Boolean,
      alarmable: Boolean,
      unlimited: Boolean
  )
  case class CorresTakebackOfferEvent(gameId: String)
  case class CorresDrawOfferEvent(gameId: String)
  case class SimulMoveEvent(
      move: MoveEvent,
      simulId: String,
      opponentUserId: String
  )
  case class NbRounds(nb: Int)
  case class Berserk(gameId: String, userId: String)
  case class IsOnGame(color: chess.Color)
  sealed trait SocketEvent
  case class TourStanding(json: JsArray)
  case class FishnetPlay(uci: Uci, currentFen: chess.format.FEN)
  case class BotPlay(playerId: String, uci: Uci, promise: Option[scala.concurrent.Promise[Unit]] = None)
  case class RematchOffer(gameId: String)
  case class RematchYes(playerId: String)
  case class RematchNo(playerId: String)
  case class Abort(playerId: String)
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
  case class Block(u1: String, u2: String)
  case class UnBlock(u1: String, u2: String)
  case class Follow(u1: String, u2: String)
}

package study {
  case class StudyDoor(userId: String, studyId: String, contributor: Boolean, public: Boolean, enters: Boolean)
  case class StudyBecamePrivate(studyId: String, contributors: Set[String])
  case class StudyBecamePublic(studyId: String, contributors: Set[String])
  case class StudyMemberGotWriteAccess(userId: String, studyId: String)
  case class StudyMemberLostWriteAccess(userId: String, studyId: String)
  case class RemoveStudy(studyId: String, contributors: Set[String])
}

package plan {
  case class ChargeEvent(username: String, amount: Int, percent: Int, date: DateTime)
  case class MonthInc(userId: String, months: Int)
}
