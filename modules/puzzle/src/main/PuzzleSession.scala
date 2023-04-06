package lila.puzzle

import chess.Color
import scala.util.chaining.*

import lila.db.dsl.*
import lila.memo.CacheApi
import lila.user.User

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

  override def toString = s"$path:$positionInPath"

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
    sessions.getIfPresent(round.userId) ?? {
      _ map { session =>
        // yes, even if the completed puzzle was not the current session puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        if (session.path.angle == angle)
          sessions.put(round.userId, fuccess(session.next))
      }
    }

  def getSettings(user: User): Fu[PuzzleSettings] =
    sessions
      .getIfPresent(user.id)
      .fold[Fu[PuzzleSettings]](fuccess(PuzzleSettings.default))(_.dmap(_.settings))

  def setDifficulty(user: User, difficulty: PuzzleDifficulty): Funit =
    updateSession(user): prev =>
      (prev.fold(true)(_.settings.difficulty != difficulty)) option
        createSessionFor(
          user,
          prev.map(_.path.angle) | PuzzleAngle.mix,
          PuzzleSettings(difficulty, prev.flatMap(_.settings.color))
        )

  def setAngleAndColor(user: User, angle: PuzzleAngle, color: Option[Color]): Funit =
    updateSession(user): prev =>
      (prev.fold(true)(p => p.settings.color != color || p.path.angle != angle)) option
        createSessionFor(
          user,
          angle,
          PuzzleSettings(prev.fold(PuzzleDifficulty.default)(_.settings.difficulty), color)
        )

  private[puzzle] def set(user: User, session: PuzzleSession) = sessions.put(user.id, fuccess(session))

  private def updateSession(user: User)(f: Option[PuzzleSession] => Option[Fu[PuzzleSession]]): Funit =
    sessions
      .getIfPresent(user.id)
      .fold(fuccess(none[PuzzleSession]))(_ dmap some)
      .flatMap { prev =>
        f(prev) match
          case Some(nextFu) =>
            nextFu map { next =>
              !prev.exists(next.similarTo) ?? sessions.put(user.id, fuccess(next))
            }
          case _ => funit
      }

  private val sessions = cacheApi.notLoading[UserId, PuzzleSession](32768, "puzzle.session")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  private[puzzle] def continueOrCreateSessionFor(
      user: User,
      angle: PuzzleAngle,
      canFlush: Boolean
  ): Fu[PuzzleSession] =
    sessions
      .getFuture(user.id, _ => createSessionFor(user, angle, PuzzleSettings.default))
      .flatMap: current =>
        if current.path.angle != angle || (canFlush && shouldFlushSession(user, current))
        then createSessionFor(user, angle, current.settings) tap { sessions.put(user.id, _) }
        else fuccess(current)

  // renew the session often for provisional players
  private def shouldFlushSession(user: User, session: PuzzleSession) = !session.brandNew && {
    val perf = user.perfs.puzzle
    perf.clueless || (perf.provisional.yes && perf.nb % 5 == 0)
  }

  private def createSessionFor(user: User, angle: PuzzleAngle, settings: PuzzleSettings): Fu[PuzzleSession] =
    pathApi
      .nextFor(user, angle, PuzzleTier.top, settings.difficulty, Set.empty)
      .orFail(s"No puzzle path found for ${user.id}, angle: $angle")
      .dmap(pathId => PuzzleSession(settings, pathId, 0))
