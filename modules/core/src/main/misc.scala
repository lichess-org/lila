package lila.core
package misc

import scalalib.bus
import lila.core.id.GameId
import lila.core.userId.*
import lila.core.user.Me

package streamer:
  case class StreamStart(userId: UserId, streamerName: String)
  object StreamStart extends bus.GivenChannel[StreamStart]("streamStart")

  case class StreamersOnline(streamers: Iterable[(UserId, String)])
  object StreamersOnline extends bus.GivenChannel[StreamersOnline]("streamersOnline")

package map:
  case class Tell(id: String, msg: Any)
  case class TellIfExists(id: String, msg: Any)
  case class TellMany(ids: Seq[String], msg: Any)
  case class TellAll(msg: Any)
  case class Exists(id: String, promise: Promise[Boolean])

package clas:
  enum ClasBus:
    case AreKidsInSameClass(kid1: UserId, kid2: UserId, promise: Promise[Boolean])
    case IsTeacherOf(teacher: UserId, student: UserId, promise: Promise[Boolean])
    case ClasMatesAndTeachers(kid: UserId, promise: Promise[Set[UserId]])
  object ClasBus extends bus.GivenChannel[ClasBus]("clas")

package puzzle:
  case class StormRun(userId: UserId, score: Int)
  object StormRun extends bus.GivenChannel[StormRun]("stormRun")

  case class RacerRun(userId: UserId, score: Int)
  object RacerRun extends bus.GivenChannel[RacerRun]("racerRun")

  case class StreakRun(userId: UserId, score: Int)
  object StreakRun extends bus.GivenChannel[StreakRun]("streakRun")

package lpv:
  import _root_.chess.format.pgn.PgnStr
  enum LpvEmbed:
    case PublicPgn(pgn: PgnStr)
    case PrivateStudy
  type LinkRender = (String, String) => Option[scalatags.Text.Frag]
  case class AllPgnsFromText(text: String, max: Max, promise: Promise[Map[String, LpvEmbed]])
  case class LpvLinkRenderFromText(text: String, promise: Promise[LinkRender])

package mailer:
  case class CorrespondenceOpponent(
      opponentId: Option[UserId],
      remainingTime: Option[java.time.Duration],
      gameId: GameId
  )
  case class CorrespondenceOpponents(userId: UserId, opponents: List[CorrespondenceOpponent])
  object CorrespondenceOpponents extends bus.GivenChannel[CorrespondenceOpponents]("dailyCorrespondenceNotif")

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
