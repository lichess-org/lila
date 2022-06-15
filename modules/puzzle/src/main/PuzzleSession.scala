package lila.puzzle

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext
import scala.util.chaining._
import chess.Color

import lila.db.dsl._
import lila.memo.CacheApi
import lila.user.{ User, UserRepo }

private case class PuzzleSession(
    difficulty: PuzzleDifficulty,
    color: Option[Color],
    path: PuzzlePath.Id,
    positionInPath: Int,
    previousPaths: Set[PuzzlePath.Id] = Set.empty
) {
  def switchTo(pathId: PuzzlePath.Id) = copy(
    path = pathId,
    previousPaths = previousPaths + pathId,
    positionInPath = 0
  )
  def next = copy(positionInPath = positionInPath + 1)

  def brandNew = positionInPath == 0

  override def toString = s"$path:$positionInPath"
}

final class PuzzleSessionApi(
    colls: PuzzleColls,
    pathApi: PuzzlePathApi,
    cacheApi: CacheApi,
    userRepo: UserRepo
)(implicit ec: ExecutionContext) {

  import BsonHandlers._

  def onComplete(round: PuzzleRound, angle: PuzzleAngle): Funit =
    sessions.getIfPresent(round.userId) ?? {
      _ map { session =>
        // yes, even if the completed puzzle was not the current session puzzle
        // in that case we just skip a puzzle on the path, which doesn't matter
        if (session.path.angle == angle)
          sessions.put(round.userId, fuccess(session.next))
      }
    }

  def getDifficulty(user: User): Fu[PuzzleDifficulty] =
    sessions
      .getIfPresent(user.id)
      .fold[Fu[PuzzleDifficulty]](fuccess(PuzzleDifficulty.default))(_.dmap(_.difficulty))

  def setDifficulty(user: User, difficulty: PuzzleDifficulty): Funit =
    updateSession(user) { prev =>
      createSessionFor(
        user,
        prev.map(_.path.angle) | PuzzleAngle.mix,
        difficulty,
        prev.flatMap(_.color)
      )
    }

  def getColor(user: User): Fu[Option[Color]] =
    sessions.getIfPresent(user.id).??(_.dmap(_.color))

  def setColor(user: User, color: Option[Color]): Funit =
    updateSession(user) { prev =>
      createSessionFor(
        user,
        prev.map(_.path.angle) | PuzzleAngle.mix,
        prev.fold(PuzzleDifficulty.default)(_.difficulty),
        color
      )
    }

  private[puzzle] def set(user: User, session: PuzzleSession) = sessions.put(user.id, fuccess(session))

  private def updateSession(user: User)(f: Option[PuzzleSession] => Fu[PuzzleSession]): Funit =
    sessions
      .getIfPresent(user.id)
      .fold(fuccess(none[PuzzleSession]))(_ dmap some)
      .flatMap { prev =>
        f(prev).tap { sessions.put(user.id, _) }.void
      }

  private val sessions = cacheApi.notLoading[User.ID, PuzzleSession](32768, "puzzle.session")(
    _.expireAfterWrite(1 hour).buildAsync()
  )

  private[puzzle] def continueOrCreateSessionFor(
      user: User,
      angle: PuzzleAngle
  ): Fu[PuzzleSession] =
    sessions.getFuture(user.id, _ => createSessionFor(user, angle)) flatMap { current =>
      if (current.path.angle == angle && !shouldChangeSession(user, current)) fuccess(current)
      else createSessionFor(user, angle, current.difficulty, current.color) tap { sessions.put(user.id, _) }
    }

  private def shouldChangeSession(user: User, session: PuzzleSession) = !session.brandNew && {
    val perf = user.perfs.puzzle
    perf.clueless || (perf.provisional && perf.nb % 5 == 0)
  }

  private def createSessionFor(
      user: User,
      angle: PuzzleAngle,
      difficulty: PuzzleDifficulty = PuzzleDifficulty.default,
      color: Option[Color] = None
  ): Fu[PuzzleSession] =
    pathApi
      .nextFor(user, angle, PuzzleTier.Top, difficulty, Set.empty)
      .orFail(s"No puzzle path found for ${user.id}, angle: $angle")
      .dmap(pathId => PuzzleSession(difficulty, color, pathId, 0))
}
