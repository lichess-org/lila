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
object Bus extends scalalib.bus.GivenChannel[Bus]("fishnet")

type AnalysisAwaiter = (Seq[GameId], FiniteDuration) => Funit

trait FishnetRequest:
  def tutor(gameId: GameId): Funit
