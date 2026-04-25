package lila.core
package misc

import scalalib.data.LazyFu
import lila.core.id.{ GameId, ClasId }
import lila.core.userId.*
import lila.core.user.Me

trait AtInstant[A]:
  def apply(a: A): Instant
  extension (a: A) inline def atInstant: Instant = apply(a)
object AtInstant:
  given atInstantOrdering: [A: AtInstant] => Ordering[A] = Ordering.by[A, Instant](_.atInstant)

package streamer:
  case class StreamStart(userId: UserId, streamerName: String)

  case class StreamInfo(name: String, lang: String)
  case class StreamersOnline(streamers: Map[UserId, StreamInfo])

package clas:

  enum ClasBus:
    case CanKidsUseMessages(kid1: UserId, kid2: UserId, promise: Promise[Boolean])
    case IsTeacherOf(teacher: UserId, student: UserId, promise: Promise[Boolean])
    case ClasMatesAndTeachers(kid: UserId, promise: Promise[Set[UserId]])

  case class ClasTeamConfig(name: String, teacherIds: NonEmptyList[UserId], studentIds: LazyFu[List[UserId]])
  case class ClasTeamUpdate(clasId: ClasId, wantsTeam: Option[ClasTeamConfig])(using val teacher: Option[Me])

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

package push:
  case class TourSoon(tourId: String, tourName: String, userIds: Iterable[UserId], swiss: Boolean)

package oauth:
  opaque type AccessTokenId = String
  object AccessTokenId extends OpaqueString[AccessTokenId]

  case class TokenRevoke(id: AccessTokenId)

package analysis:
  final class MyEnginesAsJson(val get: Option[Me] => Fu[play.api.libs.json.JsObject])

type BookmarkExists = (game.Game, Option[userId.UserId]) => Fu[Boolean]

package practice:
  import lila.core.id.{ StudyId, StudyChapterId }
  case class OnComplete(userId: UserId, studyId: StudyId, chapterId: StudyChapterId)
