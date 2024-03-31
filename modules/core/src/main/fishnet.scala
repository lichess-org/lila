package lila.core
package fishnet

import chess.format.{ Uci, Fen }

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

trait FishnetApi:
  def analyseGame(gameId: GameId): Funit
  def analyseStudyChapter(req: StudyChapterRequest): Funit
