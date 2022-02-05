package lila.analyse

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

  case class PovLike(
      color: shogi.Color,
      startColor: shogi.Color,
      startedAtPly: Int
  )

  implicit def povToPovLike(pov: Pov): PovLike =
    PovLike(
      color = pov.color,
      startColor = pov.game.startColor,
      startedAtPly = pov.game.shogi.startedAtPly
    )

  def diffsList(pov: PovLike, analysis: Analysis): List[Int] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtPly) :: analysis.infos
    else analysis.infos
  }.grouped(2)
    .foldLeft(List[Int]()) {
      case (list, List(i1, i2)) =>
        makeDiff.lift((i1.cp, i1.mate, i2.cp, i2.mate)).fold(list) { diff =>
          (if (pov.color.sente) -diff else diff).max(0) :: list
        }
      case (list, _) => list
    }
    .reverse

  def prevColorInfos(pov: PovLike, analysis: Analysis): List[Info] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtPly) :: analysis.infos
    else analysis.infos
  }.zipWithIndex.collect {
    case (e, i) if (i % 2) == 0 => e
  }

  def mean(pov: PovLike, analysis: Analysis): Option[Int] = {
    val diffs = diffsList(pov, analysis)
    val nb    = diffs.size
    (nb != 0) option (diffs.sum / nb)
  }
  def mean(pov: Pov, analysis: Analysis): Option[Int] = mean(povToPovLike(pov), analysis)
}
