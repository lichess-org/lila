package lila.analyse

import lila.core.game.{ Pov, SideAndStart }
import lila.tree.Eval.*
import lila.tree.{ Analysis, Info }

object AccuracyCP:

  def diffsList(pov: SideAndStart, analysis: Analysis): List[Option[Int]] = {
    if pov.color == pov.startColor then Info.start(pov.startedAtPly) :: analysis.infos
    else analysis.infos
  }.map(_.eval)
    .grouped(2)
    .collect { case List(e1, e2) =>
      for s1 <- e1.forceAsCp; s2 <- e2.forceAsCp yield {
        (s2.ceiled.value - s1.ceiled.value) * pov.color.fold(-1, 1)
      }.atLeast(0)
    }
    .toList

  def prevColorInfos(pov: SideAndStart, analysis: Analysis): List[Info] = {
    if pov.color == pov.startColor then Info.start(pov.startedAtPly) :: analysis.infos
    else analysis.infos
  }.zipWithIndex.collect:
    case (e, i) if (i % 2) == 0 => e

  def mean(pov: SideAndStart, analysis: Analysis): Option[Int] =
    scalalib.Maths.mean(diffsList(pov, analysis).flatten).map(x => Math.round(x).toInt)

  def mean(pov: Pov, analysis: Analysis): Option[Int] = mean(pov.sideAndStart, analysis)
