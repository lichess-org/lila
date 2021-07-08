package lila.hub
package actorApi

import chess.format.Uci
import org.joda.time.DateTime
import play.api.libs.json._
import scala.concurrent.Promise

// announce something to all clients
case class Announce(msg: String, date: DateTime, json: JsObject)

package streamer {
  case class StreamStart(userId: String)
}

package map {
  case class Tell(id: String, msg: Any)
  case class TellIfExists(id: String, msg: Any)
  case class TellMany(ids: Seq[String], msg: Any)
  case class TellAll(msg: Any)
  case class Exists(id: String, promise: Promise[Boolean])
}

package socket {

  case class SendTo(userId: String, message: JsObject)
  case class SendToAsync(userId: String, message: () => Fu[JsObject])
  object SendTo {
    def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
      SendTo(userId, Json.obj("t" -> typ, "d" -> data))
    def async[A: Writes](userId: String, typ: String, data: () => Fu[A]): SendToAsync =
      SendToAsync(userId, () => data() dmap { d => Json.obj("t" -> typ, "d" -> d) })
  }
  case class SendTos(userIds: Set[String], message: JsObject)
  object SendTos {
    def apply[A: Writes](userIds: Set[String], typ: String, data: A): SendTos =
      SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
  }
  object remote {
    case class TellSriIn(sri: String, user: Option[String], msg: JsObject)
    case class TellSriOut(sri: String, payload: JsValue)
    case class TellUserIn(user: String, msg: JsObject)
  }
  case class ApiUserIsOnline(userId: String, isOnline: Boolean)
}

package clas {
  case class AreKidsInSameClass(kid1: user.KidId, kid2: user.KidId, promise: Promise[Boolean])
  case class IsTeacherOf(teacher: String, student: String, promise: Promise[Boolean])
  case class ClasMatesAndTeachers(kid: user.KidId, promise: Promise[Set[String]])
}

package report {
  case class Cheater(userId: String, text: String)
  case class Shutup(userId: String, text: String)
  case class AutoFlag(suspectId: String, resource: String, text: String)
  case class CheatReportCreated(userId: String)
}

package security {
  case class GarbageCollect(userId: String)
  case class GCImmediateSb(userId: String)
  case class CloseAccount(userId: String)
  case class DeletePublicChats(userId: String)
}

package msg {
  case class SystemMsg(userId: String, text: String)
}

package puzzle {
  case class StormRun(userId: String, score: Int)
  case class RacerRun(userId: String, score: Int)
  case class StreakRun(userId: String, score: Int)
}

package shutup {
  case class RecordPublicForumMessage(userId: String, text: String)
  case class RecordTeamForumMessage(userId: String, text: String)
  case class RecordPrivateMessage(userId: String, toUserId: String, text: String)
  case class RecordPrivateChat(chatId: String, userId: String, text: String)
  case class RecordPublicChat(userId: String, text: String, source: PublicSource)

  sealed abstract class PublicSource(val parentName: String)
  object PublicSource {
    case class Tournament(id: String)  extends PublicSource("tournament")
    case class Simul(id: String)       extends PublicSource("simul")
    case class Study(id: String)       extends PublicSource("study")
    case class Watcher(gameId: String) extends PublicSource("watcher")
    case class Team(id: String)        extends PublicSource("team")
    case class Swiss(id: String)       extends PublicSource("swiss")
  }
}

package mod {
  case class MarkCheater(userId: String, value: Boolean)
  case class MarkBooster(userId: String)
  case class ChatTimeout(mod: String, user: String, reason: String, text: String)
  case class Shadowban(user: String, value: Boolean)
  case class KickFromRankings(userId: String)
  case class AutoWarning(userId: String, subject: String)
  case class Impersonate(userId: String, by: Option[String])
  case class SelfReportMark(userId: String, name: String)
}

package playban {
  case class Playban(userId: String, mins: Int, inTournament: Boolean)
  case class RageSitClose(userId: String)
}

package captcha {
  case object AnyCaptcha
  case class GetCaptcha(id: String)
  case class ValidCaptcha(id: String, solution: String)
}

package simul {
  case class GetHostIds(promise: Promise[Set[String]])
  case class PlayerMove(gameId: String)
}

package irc {
  sealed trait Event
  case class Error(msg: String)   extends Event
  case class Warning(msg: String) extends Event
  case class Info(msg: String)    extends Event
  case class Victory(msg: String) extends Event
}

package timeline {
  case class ReloadTimelines(userIds: List[String])

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
  case class ForumPost(userId: String, topicId: Option[String], topicName: String, postId: String)
      extends Atom(s"forum:${~topicId}", false) {
    def userIds = List(userId)
  }
  case class TourJoin(userId: String, tourId: String, tourName: String) extends Atom("tournament", true) {
    def userIds = List(userId)
  }
  case class GameEnd(playerId: String, opponent: Option[String], win: Option[Boolean], perf: String)
      extends Atom("gameEnd", true) {
    def userIds = opponent.toList
  }
  case class SimulCreate(userId: String, simulId: String, simulName: String)
      extends Atom("simulCreate", true) {
    def userIds = List(userId)
  }
  case class SimulJoin(userId: String, simulId: String, simulName: String) extends Atom("simulJoin", true) {
    def userIds = List(userId)
  }
  case class StudyLike(userId: String, studyId: String, studyName: String) extends Atom("studyLike", true) {
    def userIds = List(userId)
  }
  case class PlanStart(userId: String) extends Atom("planStart", true) {
    def userIds = List(userId)
  }
  case class PlanRenew(userId: String, months: Int) extends Atom("planRenew", true) {
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
    case class Followers(user: String)    extends Propagation
    case class Friends(user: String)      extends Propagation
    case class ExceptUser(user: String)   extends Propagation
    case class ModsOnly(value: Boolean)   extends Propagation
  }

  import propagation._

  case class Propagate(data: Atom, propagations: List[Propagation] = Nil) {
    def toUsers(ids: List[String])  = add(Users(ids))
    def toUser(id: String)          = add(Users(List(id)))
    def toFollowersOf(id: String)   = add(Followers(id))
    def toFriendsOf(id: String)     = add(Friends(id))
    def exceptUser(id: String)      = add(ExceptUser(id))
    def modsOnly(value: Boolean)    = add(ModsOnly(value))
    private def add(p: Propagation) = copy(propagations = p :: propagations)
  }
}

package tv {
  case class TvSelect(gameId: String, speed: chess.Speed, data: JsObject)
}

package notify {
  case class Notified(userId: String)
  case class NotifiedBatch(userIds: Iterable[String])
}

package team {
  case class CreateTeam(id: String, name: String, userId: String)
  case class JoinTeam(id: String, userId: String)
  case class IsLeader(id: String, userId: String, promise: Promise[Boolean])
  case class IsLeaderOf(leaderId: String, memberId: String, promise: Promise[Boolean])
  case class KickFromTeam(teamId: String, userId: String)
  case class TeamIdsJoinedBy(userId: String, promise: Promise[List[LightTeam.TeamID]])
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
      userId: String,
      unlimited: Boolean
  )
}

package user {
  case class Note(from: String, to: String, text: String, mod: Boolean)
  case class KidId(id: String)
  case class NonKidId(id: String)
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
  case class BoardDrawEvent(gameId: String)
  case class SimulMoveEvent(
      move: MoveEvent,
      simulId: String,
      opponentUserId: String
  )
  case class Berserk(gameId: String, userId: String)
  case class IsOnGame(color: chess.Color, promise: Promise[Boolean])
  case class TourStandingOld(data: JsArray)
  case class TourStanding(tourId: String, data: JsArray)
  case class FishnetPlay(uci: Uci, ply: Int)
  case object FishnetStart
  case class BotPlay(playerId: String, uci: Uci, promise: Option[scala.concurrent.Promise[Unit]] = None)
  case class RematchOffer(gameId: String)
  case class RematchYes(playerId: String)
  case class RematchNo(playerId: String)
  case class Abort(playerId: String)
  case class Resign(playerId: String)
  case class Mlat(micros: Int)
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
  case class Block(u1: String, u2: String)
  case class UnBlock(u1: String, u2: String)
  case class Follow(u1: String, u2: String)
  case class UnFollow(u1: String, u2: String)
}

package study {
  case class RemoveStudy(studyId: String, contributors: Set[String])
}

package plan {
  case class ChargeEvent(username: String, cents: Int, percent: Int, date: DateTime)
  case class MonthInc(userId: String, months: Int)
  case class PlanStart(userId: String)
  case class PlanGift(from: String, to: String, lifetime: Boolean)
  case class PlanExpire(userId: String)
}
