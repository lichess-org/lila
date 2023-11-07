package lila.hub
package actorApi

import lila.common.LpvEmbed
import chess.format.{ Uci, Fen }
import java.time.Duration
import play.api.libs.json.*

// announce something to all clients
case class Announce(msg: String, date: Instant, json: JsObject)

package streamer:
  case class StreamStart(userId: UserId, streamerName: String)

package map:
  case class Tell(id: String, msg: Any)
  case class TellIfExists(id: String, msg: Any)
  case class TellMany(ids: Seq[String], msg: Any)
  case class TellAll(msg: Any)
  case class Exists(id: String, promise: Promise[Boolean])

package socket:

  case class SendTo(userId: UserId, message: JsObject)
  case class SendToOnlineUser(userId: UserId, message: () => Fu[JsObject])
  object SendTo:
    def apply[A: Writes](userId: UserId, typ: String, data: A): SendTo =
      SendTo(userId, Json.obj("t" -> typ, "d" -> data))
    def onlineUser[A: Writes](userId: UserId, typ: String, data: () => Fu[A]): SendToOnlineUser =
      SendToOnlineUser(userId, () => data() dmap { d => Json.obj("t" -> typ, "d" -> d) })
  case class SendTos(userIds: Set[UserId], message: JsObject)
  object SendTos:
    def apply[A: Writes](userIds: Set[UserId], typ: String, data: A): SendTos =
      SendTos(userIds, Json.obj("t" -> typ, "d" -> data))
  object remote:
    case class TellSriIn(sri: String, user: Option[UserId], msg: JsObject)
    case class TellSriOut(sri: String, payload: JsValue)
    case class TellSrisOut(sris: Iterable[String], payload: JsValue)
    case class TellUserIn(user: UserId, msg: JsObject)
  case class ApiUserIsOnline(userId: UserId, isOnline: Boolean)

package clas:
  case class AreKidsInSameClass(kid1: UserId, kid2: UserId, promise: Promise[Boolean])
  case class IsTeacherOf(teacher: UserId, student: UserId, promise: Promise[Boolean])
  case class ClasMatesAndTeachers(kid: UserId, promise: Promise[Set[UserId]])

package report:
  case class Cheater(userId: UserId, text: String)
  case class Shutup(userId: UserId, text: String, critical: Boolean)
  case class AutoFlag(suspectId: UserId, resource: String, text: String, critical: Boolean)
  case class CheatReportCreated(userId: UserId)

package security:
  case class GarbageCollect(userId: UserId)
  case class CloseAccount(userId: UserId)
  case class DeletePublicChats(userId: UserId)

package msg:
  case class SystemMsg(userId: UserId, text: String)

package puzzle:
  case class StormRun(userId: UserId, score: Int)
  case class RacerRun(userId: UserId, score: Int)
  case class StreakRun(userId: UserId, score: Int)

package shutup:
  case class RecordTeamForumMessage(userId: UserId, text: String)
  case class RecordPrivateMessage(userId: UserId, toUserId: UserId, text: String)
  case class RecordPrivateChat(chatId: String, userId: UserId, text: String)
  case class RecordPublicText(userId: UserId, text: String, source: PublicSource)

  enum PublicSource(val parentName: String):
    case Tournament(id: TourId)  extends PublicSource("tournament")
    case Simul(id: SimulId)      extends PublicSource("simul")
    case Study(id: StudyId)      extends PublicSource("study")
    case Watcher(gameId: GameId) extends PublicSource("watcher")
    case Team(id: TeamId)        extends PublicSource("team")
    case Swiss(id: SwissId)      extends PublicSource("swiss")
    case Forum(id: ForumPostId)  extends PublicSource("forum")
    case Ublog(id: UblogPostId)  extends PublicSource("ublog")

package mod:
  case class MarkCheater(userId: UserId, value: Boolean)
  case class MarkBooster(userId: UserId)
  case class ChatTimeout(mod: UserId, user: UserId, reason: String, text: String)
  case class Shadowban(user: UserId, value: Boolean)
  case class KickFromRankings(userId: UserId)
  case class AutoWarning(userId: UserId, subject: String)
  case class Impersonate(userId: UserId, by: Option[UserId])
  case class SelfReportMark(userId: UserId, name: String)

package playban:
  case class Playban(userId: UserId, mins: Int, inTournament: Boolean)
  case class RageSitClose(userId: UserId)

package captcha:
  case object AnyCaptcha
  case class GetCaptcha(id: GameId)
  case class ValidCaptcha(id: GameId, solution: String)

package lpv:
  case class AllPgnsFromText(text: String, promise: Promise[Map[String, LpvEmbed]])
  case class LpvLinkRenderFromText(text: String, promise: Promise[lila.base.RawHtml.LinkRender])

package simul:
  case class GetHostIds(promise: Promise[Set[UserId]])
  case class PlayerMove(gameId: GameId)

package mailer:
  case class CorrespondenceOpponent(
      opponentId: Option[UserId],
      remainingTime: Option[Duration],
      gameId: GameId
  )
  case class CorrespondenceOpponents(userId: UserId, opponents: List[CorrespondenceOpponent])

package irc:
  enum Event:
    case Error(msg: String)
    case Warning(msg: String)
    case Info(msg: String)
    case Victory(msg: String)

package timeline:
  case class ReloadTimelines(userIds: List[UserId])

  sealed abstract class Atom(val channel: String, val okForKid: Boolean):
    def userIds: List[UserId]
  case class Follow(u1: UserId, u2: UserId) extends Atom("follow", true):
    def userIds = List(u1, u2)
  case class TeamJoin(userId: UserId, teamId: TeamId) extends Atom("teamJoin", false):
    def userIds = List(userId)
  case class TeamCreate(userId: UserId, teamId: TeamId) extends Atom("teamCreate", false):
    def userIds = List(userId)
  case class ForumPost(userId: UserId, topicId: ForumTopicId, topicName: String, postId: ForumPostId)
      extends Atom(s"forum:$topicId", false):
    def userIds = List(userId)
  case class UblogPost(userId: UserId, id: UblogPostId, slug: String, title: String)
      extends Atom(s"ublog:$id", false):
    def userIds = List(userId)
  case class TourJoin(userId: UserId, tourId: String, tourName: String) extends Atom("tournament", true):
    def userIds = List(userId)
  case class GameEnd(fullId: GameFullId, opponent: Option[UserId], win: Option[Boolean], perf: String)
      extends Atom("gameEnd", true):
    def userIds = opponent.toList
  case class SimulCreate(userId: UserId, simulId: SimulId, simulName: String)
      extends Atom("simulCreate", true):
    def userIds = List(userId)
  case class SimulJoin(userId: UserId, simulId: SimulId, simulName: String) extends Atom("simulJoin", true):
    def userIds = List(userId)
  case class StudyLike(userId: UserId, studyId: StudyId, studyName: StudyName)
      extends Atom("studyLike", true):
    def userIds = List(userId)
  case class PlanStart(userId: UserId) extends Atom("planStart", true):
    def userIds = List(userId)
  case class PlanRenew(userId: UserId, months: Int) extends Atom("planRenew", true):
    def userIds = List(userId)
  case class BlogPost(id: String, slug: String, title: String) extends Atom("blogPost", true):
    def userIds = Nil
  case class UblogPostLike(userId: UserId, id: String, title: String) extends Atom("ublogPostLike", false):
    def userIds = List(userId)
  case class StreamStart(id: UserId, name: String) extends Atom("streamStart", false):
    def userIds = List(id)

  enum Propagation:
    case Users(users: List[UserId])
    case Followers(user: UserId)
    case Friends(user: UserId)
    case WithTeam(teamId: TeamId)
    case ExceptUser(user: UserId)
    case ModsOnly(value: Boolean)

  import Propagation.*

  case class Propagate(data: Atom, propagations: List[Propagation] = Nil):
    def toUsers(ids: List[UserId])       = add(Users(ids))
    def toUser(id: UserId)               = add(Users(List(id)))
    def toFollowersOf(id: UserId)        = add(Followers(id))
    def toFriendsOf(id: UserId)          = add(Friends(id))
    def withTeam(teamId: Option[TeamId]) = teamId.fold(this)(id => add(WithTeam(id)))
    def exceptUser(id: UserId)           = add(ExceptUser(id))
    def modsOnly(value: Boolean)         = add(ModsOnly(value))
    private def add(p: Propagation)      = copy(propagations = p :: propagations)

package tv:
  case class TvSelect(gameId: GameId, speed: chess.Speed, data: JsObject)

package notify:
  case class NotifiedBatch(userIds: Iterable[UserId])

package team:
  case class CreateTeam(id: TeamId, name: String, userId: UserId)
  case class JoinTeam(id: TeamId, userId: UserId)
  case class IsLeader(id: TeamId, userId: UserId, promise: Promise[Boolean])
  case class IsLeaderOf(leaderId: UserId, memberId: UserId, promise: Promise[Boolean])
  case class IsLeaderWithCommPerm(id: TeamId, userId: UserId, promise: Promise[Boolean])
  case class KickFromTeam(teamId: TeamId, userId: UserId)
  case class LeaveTeam(teamId: TeamId, userId: UserId)
  case class TeamIdsJoinedBy(userId: UserId, promise: Promise[List[TeamId]])

package fishnet:
  case class AutoAnalyse(gameId: GameId)
  case class NewKey(userId: UserId, key: String)
  case class StudyChapterRequest(
      studyId: StudyId,
      chapterId: StudyChapterId,
      initialFen: Option[Fen.Epd],
      variant: chess.variant.Variant,
      moves: List[Uci],
      userId: UserId,
      unlimited: Boolean
  )

package user:

  import lila.common.EmailAddress
  case class ChangeEmail(id: UserId, email: EmailAddress)

package round:

  case class MoveEvent(
      gameId: GameId,
      fen: Fen.Epd,
      move: String
  )
  case class CorresMoveEvent(
      move: MoveEvent,
      playerUserId: Option[UserId],
      mobilePushable: Boolean,
      alarmable: Boolean,
      unlimited: Boolean
  )
  case class CorresTakebackOfferEvent(gameId: GameId)
  case class CorresDrawOfferEvent(gameId: GameId)
  case class BoardDrawEvent(gameId: GameId)
  case class SimulMoveEvent(move: MoveEvent, simulId: SimulId, opponentUserId: UserId)
  case class Berserk(gameId: GameId, userId: UserId)
  case class IsOnGame(color: chess.Color, promise: Promise[Boolean])
  case class TourStandingOld(data: JsArray)
  case class TourStanding(tourId: TourId, data: JsArray)
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
  case class AutoCheck(userId: UserId)
  case class Refresh(userId: UserId)

package bookmark:
  case class Toggle(gameId: GameId, userId: UserId)
  case class Remove(gameId: GameId)

package relation:
  case class Block(u1: UserId, u2: UserId)
  case class UnBlock(u1: UserId, u2: UserId)
  case class Follow(u1: UserId, u2: UserId)
  case class UnFollow(u1: UserId, u2: UserId)

package study:
  case class RemoveStudy(studyId: StudyId)

package plan:
  case class ChargeEvent(username: UserName, cents: Int, percent: Int, date: Instant)
  case class MonthInc(userId: UserId, months: Int)
  case class PlanStart(userId: UserId)
  case class PlanGift(from: UserId, to: UserId, lifetime: Boolean)
  case class PlanExpire(userId: UserId)

package push:
  case class TourSoon(tourId: String, tourName: String, userIds: Iterable[UserId], swiss: Boolean)

package oauth:
  case class TokenRevoke(id: String)
