package lila.core
package actorApi

import chess.format.{ Fen, Uci }
import chess.format.pgn.PgnStr
import play.api.libs.json.*

import java.time.Duration

// announce something to all clients
case class Announce(msg: String, date: Instant, json: JsObject)

package streamer:
  case class StreamStart(userId: UserId, streamerName: String)
  case class StreamersOnline(streamers: Iterable[(UserId, String)])

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
      SendToOnlineUser(userId, () => data().dmap { d => Json.obj("t" -> typ, "d" -> d) })
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

package security:
  case class GarbageCollect(userId: UserId)
  case class CloseAccount(userId: UserId)
  case class DeletePublicChats(userId: UserId)

package puzzle:
  case class StormRun(userId: UserId, score: Int)
  case class RacerRun(userId: UserId, score: Int)
  case class StreakRun(userId: UserId, score: Int)

package playban:
  case class Playban(userId: UserId, mins: Int, inTournament: Boolean)
  case class RageSitClose(userId: UserId)

package lpv:
  enum LpvEmbed:
    case PublicPgn(pgn: PgnStr)
    case PrivateStudy
  type LinkRender = (String, String) => Option[scalatags.Text.Frag]
  case class AllPgnsFromText(text: String, promise: Promise[Map[String, LpvEmbed]])
  case class LpvLinkRenderFromText(text: String, promise: Promise[LinkRender])

package mailer:
  case class CorrespondenceOpponent(
      opponentId: Option[UserId],
      remainingTime: Option[Duration],
      gameId: GameId
  )
  case class CorrespondenceOpponents(userId: UserId, opponents: List[CorrespondenceOpponent])

package notify:
  case class NotifiedBatch(userIds: Iterable[UserId])

package evaluation:
  case class AutoCheck(userId: UserId)
  case class Refresh(userId: UserId)

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
