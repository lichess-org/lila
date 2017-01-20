package lila.practice

case class StudyProgress(moves: StudyProgress.ChapterNbMoves) extends AnyVal {

  import StudyProgress._

  // def withScore(level: Int, s: Score) = copy(
  //   scores = (0 until scores.size.max(level)).map { i =>
  //     scores.lift(i) | Score(0)
  //   }.updated(level - 1, s).toVector)
}

object StudyProgress {

  type ChapterNbMoves = Map[lila.study.Chapter.Id, StudyProgress.NbMoves]

  def empty = StudyProgress(moves = Map.empty)

  case class NbMoves(value: Int) extends AnyVal
  implicit val nbMovesIso = lila.common.Iso.int[NbMoves](NbMoves.apply, _.value)
}
