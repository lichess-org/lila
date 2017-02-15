package lila.analyse

import scalaz.NonEmptyList

import lila.game.Pov
import lila.tree.Eval._

object Accuracy {

  private def withSignOf(i: Int, signed: Int) = if (signed < 0) -i else i

  private val makeDiff: PartialFunction[(Option[Cp], Option[Mate], Option[Cp], Option[Mate]), Int] = {
    case (Some(s1), _, Some(s2), _) => s2.ceiled.centipawns - s1.ceiled.centipawns
    case (Some(s1), _, _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - s1.ceiled.centipawns
    case (_, Some(m1), Some(s2), _) => s2.ceiled.centipawns - withSignOf(Cp.CEILING, m1.value)
    case (_, Some(m1), _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - withSignOf(Cp.CEILING, m1.value)
  }

  def diffsList(pov: Pov, analysis: AnyAnalysis): List[Int] =
    povInfos(pov, analysis).list.grouped(2).foldLeft(List.empty[Int]) {
      case (list, List(Some(i1), Some(i2))) =>
        makeDiff.lift(i1.cp, i1.mate, i2.cp, i2.mate).fold(list) { diff =>
          (if (pov.color.white) -diff else diff).max(0) :: list
        }
      case (list, _) => list
    }.reverse

  def prevColorInfos(pov: Pov, analysis: AnyAnalysis): List[Option[Info]] =
    povInfos(pov, analysis).list.zipWithIndex.collect {
      case (e, i) if i % 2 == 0 => e
    }

  private def povInfos(pov: Pov, analysis: AnyAnalysis): NonEmptyList[Option[Info]] =
    if (pov.color == pov.game.startColor)
      Info.start(pov.game.startedAtTurn).some <:: analysis.infoOptions
    else analysis.infoOptions

  def mean(pov: Pov, analysis: AnyAnalysis): Option[Int] = {
    val diffs = diffsList(pov, analysis)
    val nb = diffs.size
    (nb != 0) option (diffs.sum / nb)
  }
}
