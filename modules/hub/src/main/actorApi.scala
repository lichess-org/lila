package lila.hub
package actorApi

import chess.format.Uci
import org.joda.time.{ DateTime, Period }
import play.api.libs.json.*
import scala.concurrent.Promise

// announce something to all clients
case class Announce(msg: String, date: DateTime, json: JsObject)

package streamer:
  case class StreamStart(userId: String)

package map:
  case class Tell(id: String, msg: Any)
  case class TellIfExists(id: String, msg: Any)
  case class TellMany(ids: Seq[String], msg: Any)
  case class TellAll(msg: Any)
  case class Exists(id: String, promise: Promise[Boolean])

package socket:

  case class SendTo(userId: String, message: JsObject)
  case class SendToAsync(userId: String, message: () => Fu[JsObject])
  object SendTo:
    def apply[A: Writes](userId: String, typ: String, data: A): SendTo =
      SendTo(userId, Json.obj("t" -> typ, "d" -> data))
    def async[A: Writes](userId: String, typ: String, data: () => Fu[A]): SendToAsync =
      SendToAsync(userId, () => data() dmap { d => Json.obj("t" -> typ, "d" -> d) })
  case class SendTos(userIds: Set[String], message: JsObject)
  object SendTos:
    def apply[A: Writes](userIds: Set[String], typ: String, data: A): SendTos =
      SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
  object remote:
    case class TellSriIn(sri: String, user: Option[String], msg: JsObject)
    case class TellSriOut(sri: String, payload: JsValue)
    case class TellUserIn(user: String, msg: JsObject)
  case class ApiUserIsOnline(userId: String, isOnline: Boolean)

package clas:
  case class AreKidsInSameClass(kid1: user.KidId, kid2: user.KidId, promise: Promise[Boolean])
  case class IsTeacherOf(teacher: String, student: String, promise: Promise[Boolean])
  case class ClasMatesAndTeachers(kid: user.KidId, promise: Promise[Set[String]])

package report:
  case class Cheater(userId: String, text: String)
  case class Shutup(userId: String, text: String, critical: Boolean)
  case class AutoFlag(suspectId: String, resource: String, text: String)
  case class CheatReportCreated(userId: String)

package security:
  case class GarbageCollect(userId: String)
  case class CloseAccount(userId: String)
  case class DeletePublicChats(userId: String)

package msg:
  case class SystemMsg(userId: String, text: String)

package puzzle:
  case class StormRun(userId: String, score: Int)
  case class RacerRun(userId: String, score: Int)
  case class StreakRun(userId: String, score: Int)

package shutup:
  case class RecordPublicForumMessage(userId: String, text: String)
  case class RecordTeamForumMessage(userId: String, text: String)
  case class RecordPrivateMessage(userId: String, toUserId: String, text: String)
  case class RecordPrivateChat(chatId: String, userId: String, text: String)
  case class RecordPublicChat(userId: String, text: String, source: PublicSource)

  sealed abstract class PublicSource(val parentName: String)
  object PublicSource:
    case class Tournament(id: TourId)  extends PublicSource("tournament")
    case class Simul(id: SimulId)      extends PublicSource("simul")
    case class Study(id: StudyId)      extends PublicSource("study")
    case class Watcher(gameId: GameId) extends PublicSource("watcher")
    case class Team(id: TeamId)        extends PublicSource("team")
    case class Swiss(id: SwissId)      extends PublicSource("swiss")

package mod:
  case class MarkCheater(userId: String, value: Boolean)
  case class MarkBooster(userId: String)
  case class ChatTimeout(mod: String, user: String, reason: String, text: String)
  case class Shadowban(user: String, value: Boolean)
  case class KickFromRankings(userId: String)
  case class AutoWarning(userId: String, subject: String)
  case class Impersonate(userId: String, by: Option[String])
  case class SelfReportMark(userId: String, name: String)

package playban:
  case class Playban(userId: String, mins: Int, inTournament: Boolean)
  case class RageSitClose(userId: String)

package captcha:
  case object AnyCaptcha
  case class GetCaptcha(id: GameId)
  case class ValidCaptcha(id: GameId, solution: String)

package lpv:
  case class GamePgnsFromText(text: String, promise: Promise[Map[GameId, String]])
  case class LpvLinkRenderFromText(text: String, promise: Promise[lila.base.RawHtml.LinkRender])

package simul:
  case class GetHostIds(promise: Promise[Set[String]])
  case class PlayerMove(gameId: GameId)

package mailer:
  case class CorrespondenceOpponent(opponentId: Option[String], remainingTime: Option[Period], gameId: GameId)
  case class CorrespondenceOpponents(userId: String, opponents: List[CorrespondenceOpponent])

package irc:
  sealed trait Event
  case class Error(msg: String)   extends Event
  case class Warning(msg: String) extends Event
  case class Info(msg: String)    extends Event
  case class Victory(msg: String) extends Event

package timeline:
  case class ReloadTimelines(userIds: List[String])

  sealed abstract class Atom(val channel: String, val okForKid: Boolean):
    def userIds: List[String]
  case class Follow(u1: String, u2: String) extends Atom("follow", true):
    def userIds = List(u1, u2)
  case class TeamJoin(userId: String, teamId: TeamId) extends Atom("teamJoin", false):
    def userIds = List(userId)
  case class TeamCreate(userId: String, teamId: TeamId) extends Atom("teamCreate", false):
    def userIds = List(userId)
  case class ForumPost(userId: String, topicId: Option[String], topicName: String, postId: String)
      extends Atom(s"forum:${~topicId}", false):
    def userIds = List(userId)
  case class UblogPost(userId: String, id: UblogPostId, slug: String, title: String)
      extends Atom(s"ublog:$id", false):
    def userIds = List(userId)
  case class TourJoin(userId: String, tourId: String, tourName: String) extends Atom("tournament", true):
    def userIds = List(userId)
  case class GameEnd(fullId: GameFullId, opponent: Option[String], win: Option[Boolean], perf: String)
      extends Atom("gameEnd", true):
    def userIds = opponent.toList
  case class SimulCreate(userId: String, simulId: SimulId, simulName: String)
      extends Atom("simulCreate", true):
    def userIds = List(userId)
  case class SimulJoin(userId: String, simulId: SimulId, simulName: String) extends Atom("simulJoin", true):
    def userIds = List(userId)
  case class StudyLike(userId: String, studyId: StudyId, studyName: StudyName)
      extends Atom("studyLike", true):
    def userIds = List(userId)
  case class PlanStart(userId: String) extends Atom("planStart", true):
    def userIds = List(userId)
  case class PlanRenew(userId: String, months: Int) extends Atom("planRenew", true):
    def userIds = List(userId)
  case class BlogPost(id: String, slug: String, title: String) extends Atom("blogPost", true):
    def userIds = Nil
  case class UblogPostLike(userId: String, id: String, title: String) extends Atom("ublogPostLike", false):
    def userIds = List(userId)
  case class StreamStart(id: String, name: String) extends Atom("streamStart", false):
    def userIds = List(id)

  object propagation:
    sealed trait Propagation
    case class Users(users: List[String]) extends Propagation
    case class Followers(user: String)    extends Propagation
    case class Friends(user: String)      extends Propagation
    case class WithTeam(teamId: TeamId)   extends Propagation
    case class ExceptUser(user: String)   extends Propagation
    case class ModsOnly(value: Boolean)   extends Propagation

  import propagation.*

  case class Propagate(data: Atom, propagations: List[Propagation] = Nil):
    def toUsers(ids: List[String])       = add(Users(ids))
    def toUser(id: String)               = add(Users(List(id)))
    def toFollowersOf(id: String)        = add(Followers(id))
    def toFriendsOf(id: String)          = add(Friends(id))
    def withTeam(teamId: Option[TeamId]) = teamId.fold(this)(id => add(WithTeam(id)))
    def exceptUser(id: String)           = add(ExceptUser(id))
    def modsOnly(value: Boolean)         = add(ModsOnly(value))
    private def add(p: Propagation)      = copy(propagations = p :: propagations)

package tv:
  case class TvSelect(gameId: GameId, speed: chess.Speed, data: JsObject)

package notify:
  case class NotifiedBatch(userIds: Iterable[String])

package team:
  case class CreateTeam(id: TeamId, name: String, userId: String)
  case class JoinTeam(id: TeamId, userId: String)
  case class IsLeader(id: TeamId, userId: String, promise: Promise[Boolean])
  case class IsLeaderOf(leaderId: String, memberId: String, promise: Promise[Boolean])
  case class KickFromTeam(teamId: TeamId, userId: String)
  case class LeaveTeam(teamId: TeamId, userId: String)
  case class TeamIdsJoinedBy(userId: String, promise: Promise[List[TeamId]])

package fishnet:
  case class AutoAnalyse(gameId: GameId)
  case class NewKey(userId: String, key: String)
  case class StudyChapterRequest(
      studyId: StudyId,
      chapterId: StudyChapterId,
      initialFen: Option[chess.format.FEN],
      variant: chess.variant.Variant,
      moves: List[Uci],
      userId: String,
      unlimited: Boolean
  )

package user:

  import lila.common.EmailAddress
  case class Note(from: String, to: String, text: String, mod: Boolean)
  sealed trait ClasId
  case class KidId(id: String)    extends ClasId
  case class NonKidId(id: String) extends ClasId
  case class ChangeEmail(id: String, email: EmailAddress)

package round:

  case class MoveEvent(
      gameId: GameId,
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
  case class CorresTakebackOfferEvent(gameId: GameId)
  case class CorresDrawOfferEvent(gameId: GameId)
  case class BoardDrawEvent(gameId: GameId)
  case class SimulMoveEvent(move: MoveEvent, simulId: SimulId, opponentUserId: String)
  case class Berserk(gameId: GameId, userId: String)
  case class IsOnGame(color: chess.Color, promise: Promise[Boolean])
  case class TourStandingOld(data: JsArray)
  case class TourStanding(tourId: String, data: JsArray)
  case class FishnetPlay(uci: Uci, sign: String)
  case object FishnetStart
  case class BotPlay(playerId: GamePlayerId, uci: Uci, promise: Option[Promise[Unit]] = None)
  case class RematchOffer(gameId: GameId)
  case class RematchCancel(gameId: GameId)
  case class RematchYes(playerId: GamePlayerId)
  case class RematchNo(playerId: GamePlayerId)
  case class Abort(playerId: GamePlayerId)
  case class Resign(playerId: GamePlayerId)
  case class Mlat(millis: Int)

package evaluation:
  case class AutoCheck(userId: String)
  case class Refresh(userId: String)

package bookmark:
  case class Toggle(gameId: GameId, userId: String)
  case class Remove(gameId: GameId)

package relation:
  case class Block(u1: String, u2: String)
  case class UnBlock(u1: String, u2: String)
  case class Follow(u1: String, u2: String)
  case class UnFollow(u1: String, u2: String)

package study:
  case class RemoveStudy(studyId: StudyId, contributors: Set[String])

package plan:
  case class ChargeEvent(username: String, cents: Int, percent: Int, date: DateTime)
  case class MonthInc(userId: String, months: Int)
  case class PlanStart(userId: String)
  case class PlanGift(from: String, to: String, lifetime: Boolean)
  case class PlanExpire(userId: String)

package push:
  case class TourSoon(tourId: String, tourName: String, userIds: Iterable[String], swiss: Boolean)

package oauth:
  case class TokenRevoke(id: String)
