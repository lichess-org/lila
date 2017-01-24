package lila.practice

sealed trait PracticeGoal

object PracticeGoal {

  case object Checkmate extends PracticeGoal
  case class DrawIn(nbMoves: Int) extends PracticeGoal
  case class EvalIn(cp: Int, nbMoves: Int) extends PracticeGoal

  val DrawInRegex = """(?i)draw in (\d+)""".r
  val EvalInRegex = """(?i)((?:\+|-|)\d+)cp in (\d+)""".r

  def apply(chapter: lila.study.Chapter): PracticeGoal =
    chapter.tags.find(_.name == chess.format.pgn.Tag.Termination).map(_.value).flatMap {
      case DrawInRegex(movesStr) => parseIntOption(movesStr) map DrawIn.apply
      case EvalInRegex(cpStr, movesStr) => for {
        cp <- parseIntOption(cpStr)
        moves <- parseIntOption(movesStr)
      } yield EvalIn(cp, moves)
      case _ => none
    } | Checkmate
}
