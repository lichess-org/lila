package lila.practice

import lila.study.Chapter

case class StudyProgress(moves: StudyProgress.ChapterNbMoves) extends AnyVal {

  import StudyProgress._

  def withNbMoves(chapterId: Chapter.Id, nbMoves: NbMoves) = copy(
    moves = moves - chapterId + (chapterId -> nbMoves)
  )

  def get = moves.get _
}

object StudyProgress {

  type ChapterNbMoves = Map[Chapter.Id, StudyProgress.NbMoves]

  def empty = StudyProgress(moves = Map.empty)

  case class NbMoves(value: Int) extends AnyVal
  implicit val nbMovesIso = lila.common.Iso.int[NbMoves](NbMoves.apply, _.value)
}
