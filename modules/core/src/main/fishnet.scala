package lila.core
package fishnet

import _root_.chess.format.{ Fen, Uci }

import lila.core.id.{ GameId, StudyChapterId, StudyId }
import lila.core.userId.UserId

val maxPlies = 300

case class NewKey(userId: UserId, key: String)

enum Bus:
  case GameRequest(gameId: GameId)
  case StudyChapterRequest(
      studyId: StudyId,
      chapterId: StudyChapterId,
      initialFen: Option[Fen.Full],
      variant: _root_.chess.variant.Variant,
      moves: List[Uci],
      userId: UserId,
      official: Boolean
  )

type AnalysisAwaiter = (Seq[GameId], FiniteDuration) => Fu[Int]

trait FishnetRequest:
  def tutor(gameId: GameId): Funit

case class FishnetMoveRequest(game: lila.core.game.Game)
