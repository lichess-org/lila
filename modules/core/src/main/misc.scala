package lila.core
package misc

import lila.core.id.GameId
import lila.core.userId.*
import lila.core.user.Me

package streamer:
  case class StreamStart(userId: UserId, streamerName: String)

  case class StreamInfo(name: String, lang: String)
  case class StreamersOnline(streamers: Map[UserId, StreamInfo])

package clas:
  enum ClasBus:
    case AreKidsInSameClass(kid1: UserId, kid2: UserId, promise: Promise[Boolean])
    case IsTeacherOf(teacher: UserId, student: UserId, promise: Promise[Boolean])
    case ClasMatesAndTeachers(kid: UserId, promise: Promise[Set[UserId]])

package puzzle:
  case class StormRun(userId: UserId, score: Int)

  case class RacerRun(userId: UserId, score: Int)

  case class StreakRun(userId: UserId, score: Int)

package lpv:
  import _root_.chess.format.pgn.PgnStr
  enum LpvEmbed:
    case PublicPgn(pgn: PgnStr)
    case PrivateStudy
  type LinkRender = (String, String) => Option[scalatags.Text.Frag]
  enum Lpv:
    case AllPgnsFromText(text: String, max: Max, promise: Promise[Map[String, LpvEmbed]])
    case LinkRenderFromText(text: String, promise: Promise[LinkRender])

package mailer:
  case class CorrespondenceOpponent(
      opponentId: Option[UserId],
      remainingTime: Option[java.time.Duration],
      gameId: GameId
  )
  case class CorrespondenceOpponents(userId: UserId, opponents: List[CorrespondenceOpponent])

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

package analysis:
  final class MyEnginesAsJson(val get: Option[Me] => Fu[play.api.libs.json.JsObject])

trait PicfitUrl:
  def thumbnail(id: lila.core.id.ImageId, width: Int, height: Int): String
  def resize(id: lila.core.id.ImageId, size: Either[Int, Int]): String
  def raw(id: lila.core.id.ImageId): String

type BookmarkExists = (game.Game, Option[userId.UserId]) => Fu[Boolean]
