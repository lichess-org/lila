package lila.practice

import chess.format.pgn.Tag

sealed trait PracticeGoal

object PracticeGoal {

  case object Mate extends PracticeGoal
  case class MateIn(nbMoves: Int) extends PracticeGoal
  case class DrawIn(nbMoves: Int) extends PracticeGoal
  case class EvalIn(cp: Int, nbMoves: Int) extends PracticeGoal
  case class Promotion(cp: Int) extends PracticeGoal

  private val MateR = """(?i)^(?:check)?mate$""".r
  private val MateInR = """(?i)^(?:check)?mate in (\d+)$""".r
  private val DrawInR = """(?i)^draw in (\d+)$""".r
  private val EvalInR = """(?i)^((?:\+|-|)\d+)cp in (\d+)$""".r
  private val PromotionR = """(?i)^promotion with ((?:\+|-|)\d+)cp$""".r

  private val MultiSpaceR = """\s{2,}""".r

  private def tagText(tag: Tag) = MultiSpaceR.replaceAllIn(tag.value.trim, " ")

  def apply(chapter: lila.study.Chapter): PracticeGoal =
    chapter.tags.find(_.name == Tag.Termination).map(tagText).flatMap {
      case MateR()           => Mate.some
      case MateInR(movesStr) => parseIntOption(movesStr) map MateIn.apply
      case DrawInR(movesStr) => parseIntOption(movesStr) map DrawIn.apply
      case EvalInR(cpStr, movesStr) => for {
        cp <- parseIntOption(cpStr)
        moves <- parseIntOption(movesStr)
      } yield EvalIn(cp, moves)
      case PromotionR(cpStr) => parseIntOption(cpStr) map Promotion.apply
      case _                 => none
    } | Mate // default to mate
}
