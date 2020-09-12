package lila.practice

sealed trait PracticeGoal

object PracticeGoal {

  case object Mate                         extends PracticeGoal
  case class MateIn(nbMoves: Int)          extends PracticeGoal
  case class DrawIn(nbMoves: Int)          extends PracticeGoal
  case class EqualIn(nbMoves: Int)         extends PracticeGoal // same as draw, except wording
  case class EvalIn(cp: Int, nbMoves: Int) extends PracticeGoal
  case class Promotion(cp: Int)            extends PracticeGoal

  private val MateR      = """(?i)(?:check)?+mate""".r
  private val MateInR    = """(?i)(?:check)?+mate in (\d++)""".r
  private val DrawInR    = """(?i)draw in (\d++)""".r
  private val EqualInR   = """(?i)equal(?:ize)?+ in (\d++)""".r
  private val EvalInR    = """(?i)((?:\+|-|)\d++)cp in (\d++)""".r
  private val PromotionR = """(?i)promotion with ((?:\+|-|)\d++)cp""".r

  private val MultiSpaceR = """\s{2,}+""".r

  def apply(chapter: lila.study.Chapter): PracticeGoal =
    chapter.tags(_.Termination).map(v => MultiSpaceR.replaceAllIn(v.trim, " ")).flatMap {
      case MateR()            => Mate.some
      case MateInR(movesStr)  => movesStr.toIntOption map MateIn.apply
      case DrawInR(movesStr)  => movesStr.toIntOption map DrawIn.apply
      case EqualInR(movesStr) => movesStr.toIntOption map EqualIn.apply
      case EvalInR(cpStr, movesStr) =>
        for {
          cp    <- cpStr.toIntOption
          moves <- movesStr.toIntOption
        } yield EvalIn(cp, moves)
      case PromotionR(cpStr) => cpStr.toIntOption map Promotion.apply
      case _                 => none
    } | Mate // default to mate
}
