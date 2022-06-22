package lila.analyse

import lila.game.Game.SideAndStart
import lila.tree.Eval
import lila.tree.Eval.{ Cp, Mate }

// Quality of a move, based on previous and next WinPercent
case class AccuracyPercent(value: Double) extends AnyVal {
  def toInt = value.toInt
}

object AccuracyPercent {

  import WinPercent.BeforeAfter

  // FIXME https://graphsketch.com/?eqn1_color=1&eqn1_eqn=100%20-%20(sqrt(x)%20*%2010)&eqn2_color=2&eqn2_eqn=&eqn3_color=3&eqn3_eqn=&eqn4_color=4&eqn4_eqn=&eqn5_color=5&eqn5_eqn=&eqn6_color=6&eqn6_eqn=&x_min=0&x_max=100&y_min=0&y_max=100&x_tick=1&y_tick=1&x_label_freq=5&y_label_freq=5&do_grid=0&do_grid=1&bold_labeled_lines=0&bold_labeled_lines=1&line_width=4&image_w=850&image_h=525
  def fromWinPercents(before: WinPercent, after: WinPercent): AccuracyPercent = AccuracyPercent {
    if (before.value < after.value) 100d
    else 100 - Math.sqrt(before.value - after.value) * 10
  }

  def fromWinPercents(both: BeforeAfter): AccuracyPercent =
    fromWinPercents(both.before, both.after)

  def fromEvalsAndPov(pov: SideAndStart, evals: List[Eval]): List[AccuracyPercent] = {
    val subjectiveEvals = pov.color.fold(evals, evals.map(_.invert))
    val alignedEvals    = if (pov.color == pov.startColor) Eval.initial :: evals else evals
    alignedEvals
      .grouped(2)
      .collect { case List(e1, e2) =>
        for {
          before <- WinPercent.fromEval(e1)
          after  <- WinPercent.fromEval(e2)
        } yield AccuracyPercent.fromWinPercents(before, after)
      }
      .flatten
      .toList
  }

  def fromAnalysisAndPov(pov: SideAndStart, analysis: Analysis): List[AccuracyPercent] =
    fromEvalsAndPov(pov, analysis.infos.map(_.eval))
}
