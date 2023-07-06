package lila.puzzle

import chess.Color
import scala.util.chaining.*

import lila.memo.CacheApi
import lila.user.{ User, Me }
import lila.rating.Perf

private case class PuzzleSession(
    settings: PuzzleSettings,
    path: PuzzlePath.Id,
    positionInPath: Int,
    previousPaths: Set[PuzzlePath.Id] = Set.empty
):
  def switchTo(pathId: PuzzlePath.Id) = copy(
    path = pathId,
    previousPaths = previousPaths + pathId,
    positionInPath = 0
  )
  def next = copy(positionInPath = positionInPath + 1)

  def brandNew = positionInPath == 0

  def similarTo(other: PuzzleSession) =
    path.angle == other.path.angle &&
      settings.difficulty == other.settings.difficulty &&
      settings.color == other.settings.color

  override def toString = s"$path:$positionInPath($settings)"

case class PuzzleSettings(
    difficulty: PuzzleDifficulty,
    color: Option[Color]
)
object PuzzleSettings:
  val default                       = PuzzleSettings(PuzzleDifficulty.default, none)
  def default(color: Option[Color]) = PuzzleSettings(PuzzleDifficulty.default, color)

final class PuzzleSessionApi(pathApi: PuzzlePathApi, cacheApi: CacheApi)(using Executor):

  import BsonHandlers.*

  def onComplete(round: PuzzleRound, angle: PuzzleAngle): Funit =
    sessions.getIfPresent(round.userId) so {
      _ map { session =>
        // yes, even if the completed puzzle was not the current session puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        if session.path.angle == angle then sessions.put(round.userId, fuccess(session.next))
      }
    }

  def getSettings(user: User): Fu[PuzzleSettings] =
    sessions
      .getIfPresent(user.id)
      .fold[Fu[PuzzleSettings]](fuccess(PuzzleSettings.default))(_.dmap(_.settings))

  def setDifficulty(difficulty: PuzzleDifficulty)(using Me, Perf): Funit =
    updateSession: prev =>
      (prev.fold(true)(_.settings.difficulty != difficulty)) option
        createSessionFor(
          prev.map(_.path.angle) | PuzzleAngle.mix,
          PuzzleSettings(difficulty, prev.flatMap(_.settings.color))
        )

  def setAngleAndColor(angle: PuzzleAngle, color: Option[Color])(using Me, Perf): Funit =
    updateSession: prev =>
      (prev.fold(true)(p => p.settings.color != color || p.path.angle != angle)) option
        createSessionFor(
          angle,
          PuzzleSettings(prev.fold(PuzzleDifficulty.default)(_.settings.difficulty), color)
        )

  private[puzzle] def set(session: PuzzleSession)(using me: Me) = sessions.put(me.userId, fuccess(session))

  private def updateSession(f: Option[PuzzleSession] => Option[Fu[PuzzleSession]])(using me: Me): Funit =
    sessions
      .getIfPresent(me.userId)
      .fold(fuccess(none[PuzzleSession]))(_ dmap some)
      .flatMap: prev =>
        f(prev).so:
          _.map: next =>
            !prev.exists(next.similarTo) so sessions.put(me.userId, fuccess(next))

  private val sessions = cacheApi.notLoading[UserId, PuzzleSession](32768, "puzzle.session"):
    _.expireAfterWrite(1 hour).buildAsync()

  private[puzzle] def continueOrCreateSessionFor(
      angle: PuzzleAngle,
      canFlush: Boolean
  )(using me: Me, perf: Perf): Fu[PuzzleSession] =
    sessions
      .getFuture(me.userId, _ => createSessionFor(angle, PuzzleSettings.default))
      .flatMap: current =>
        if current.path.angle != angle || (canFlush && shouldFlushSession(current))
        then createSessionFor(angle, current.settings) tap { sessions.put(me.userId, _) }
        else fuccess(current)

  // renew the session often for provisional players
  private def shouldFlushSession(session: PuzzleSession)(using perf: Perf) = !session.brandNew && {
    perf.clueless || (perf.provisional.yes && perf.nb % 5 == 0)
  }

  private def createSessionFor(angle: PuzzleAngle, settings: PuzzleSettings)(using
      me: Me,
      perf: Perf
  ): Fu[PuzzleSession] =
    pathApi
      .nextFor(angle, PuzzleTier.top, settings.difficulty, Set.empty)
      .orFail(s"No puzzle path found for ${me.username}, angle: $angle")
      .dmap(pathId => PuzzleSession(settings, pathId, 0))
