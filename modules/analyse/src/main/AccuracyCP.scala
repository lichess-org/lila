package lila.analyse

import chess.Color

import lila.game.Pov
import lila.game.Game.SideAndStart
import lila.tree.Eval._

object AccuracyCP {

  private def withSignOf(i: Int, signed: Int) = if (signed < 0) -i else i

  private val makeDiff: PartialFunction[(Option[Cp], Option[Mate], Option[Cp], Option[Mate]), Int] = {
    case (Some(s1), _, Some(s2), _) => s2.ceiled.centipawns - s1.ceiled.centipawns
    case (Some(s1), _, _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - s1.ceiled.centipawns
    case (_, Some(m1), Some(s2), _) => s2.ceiled.centipawns - withSignOf(Cp.CEILING, m1.value)
    case (_, Some(m1), _, Some(m2)) => withSignOf(Cp.CEILING, m2.value) - withSignOf(Cp.CEILING, m1.value)
  }

  def diffsList(pov: SideAndStart, analysis: Analysis): List[Option[Int]] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtTurn) :: analysis.infos
    else analysis.infos
  }.map(_.eval)
    .grouped(2)
    .collect { case List(e1, e2) =>
      for { s1 <- e1.forceAsCp; s2 <- e2.forceAsCp } yield {
        (s2.ceiled.value - s1.ceiled.value) * pov.color.fold(-1, 1)
      } atLeast 0
    }
    .toList

  def prevColorInfos(pov: SideAndStart, analysis: Analysis): List[Info] = {
    if (pov.color == pov.startColor) Info.start(pov.startedAtTurn) :: analysis.infos
    else analysis.infos
  }.zipWithIndex.collect {
    case (e, i) if (i % 2) == 0 => e
  }

  def mean(pov: SideAndStart, analysis: Analysis): Option[Int] =
    lila.common.Maths.mean(diffsList(pov, analysis).flatten).map(x => Math.round(x).toInt)

  def mean(pov: Pov, analysis: Analysis): Option[Int] = mean(pov.sideAndStart, analysis)
}
