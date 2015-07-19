package lila.analyse

import lila.game.Pov

object Accuracy {

  def withSignOf(i: Int, signed: Int) = if (signed < 0) -i else i

  private val makeDiff: PartialFunction[(Option[Score], Option[Int], Option[Score], Option[Int]), Int] = {
    case (Some(s1), _, Some(s2), _) => s2.ceiled.centipawns - s1.ceiled.centipawns
    case (Some(s1), _, _, Some(m2)) => withSignOf(Score.CEILING, m2) - s1.ceiled.centipawns
    case (_, Some(m1), Some(s2), _) => s2.ceiled.centipawns - withSignOf(Score.CEILING, m1)
    case (_, Some(m1), _, Some(m2)) => withSignOf(Score.CEILING, m2) - withSignOf(Score.CEILING, m1)
  }

  def diffsList(pov: Pov, analysis: Analysis): List[Int] = (pov.color == pov.game.startColor).fold(
    Info.start(pov.game.startedAtTurn) :: analysis.infos,
    analysis.infos
  ).grouped(2).foldLeft(List[Int]()) {
      case (list, List(i1, i2)) =>
        makeDiff.lift(i1.score, i1.mate, i2.score, i2.mate).fold(list) { diff =>
          (if (pov.color.white) -diff else diff).max(0) :: list
        }
      case (list, _) => list
    }

  def apply(pov: Pov, analysis: Analysis): Option[Int] = {
    val diffs = diffsList(pov, analysis)
    val nb = diffs.size
    (nb != 0) option (diffs.sum / nb)
  }

  case class DividedAccuracy(
    all: Int,
    opening: Int,
    middle: Option[Int],
    end: Option[Int])

  def apply(pov: Pov, analysis: Analysis, div: chess.Division): Option[DividedAccuracy] = {
    val diffs = diffsList(pov, analysis)
    val openingDiffs = div.middle.fold(diffs)(m => diffs.take(m / 2))
    val middleDiffs = div.middle.?? { m =>
      div.end.fold(diffs.drop(m / 2)) { e =>
        diffs.drop(m / 2).take((e - m) / 2)
      }
    }
    val endDiffs = div.end.?? { e =>
      diffs.drop(e / 2)
    }
    diffs.nonEmpty option DividedAccuracy(
      all = (diffs.sum / diffs.size),
      opening = openingDiffs.sum / openingDiffs.size,
      middle = middleDiffs.nonEmpty option (middleDiffs.sum / middleDiffs.size),
      end = endDiffs.nonEmpty option (endDiffs.sum / endDiffs.size)
    )
  }
}
