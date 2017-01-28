package lila.practice

sealed trait PracticeGoal

object PracticeGoal {

  case object Mate extends PracticeGoal
  case class MateIn(nbMoves: Int) extends PracticeGoal
  case class DrawIn(nbMoves: Int) extends PracticeGoal
  case class EvalIn(cp: Int, nbMoves: Int) extends PracticeGoal

  val MateRegex = """(?i)^(?:check)?mate$""".r
  val MateInRegex = """(?i)^(?:check)?mate in (\d+)$""".r
  val DrawInRegex = """(?i)^draw in (\d+)$""".r
  val EvalInRegex = """(?i)^((?:\+|-|)\d+)cp in (\d+)$""".r

  def apply(chapter: lila.study.Chapter): PracticeGoal =
    chapter.tags.find(_.name == chess.format.pgn.Tag.Termination).map(_.value.trim).flatMap {
      case MateRegex()           => Mate.some
      case MateInRegex(movesStr) => parseIntOption(movesStr) map MateIn.apply
      case DrawInRegex(movesStr) => parseIntOption(movesStr) map DrawIn.apply
      case EvalInRegex(cpStr, movesStr) => for {
        cp <- parseIntOption(cpStr)
        moves <- parseIntOption(movesStr)
      } yield EvalIn(cp, moves)
      case _ => none
    } | Mate // default to mate
}
