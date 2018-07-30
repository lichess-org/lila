package lila.evaluation

import chess.{ Centis, Stats }
import lila.common.Maths

object Statistics {

  // Coefficient of Variance
  def coefVariation(a: List[Int]): Option[Float] = {
    val s = Stats(a)
    s.stdDev.map { _ / s.mean }
  }

  // ups all values by 0.5s
  // as to avoid very high variation on bullet games
  // where all move times are low (https://lichess.org/@/AlisaP?mod)
  // and drops the first move because it's always 0
  def moveTimeCoefVariation(a: List[Centis]): Option[Float] =
    coefVariation(a.drop(1).map(_.centis + 50))

  def moveTimeCoefVariationNoDrop(a: List[Centis]): Option[Float] =
    coefVariation(a.map(_.centis + 50))

  def moveTimeCoefVariation(pov: lila.game.Pov): Option[Float] =
    for {
      mt <- moveTimes(pov)
      coef <- moveTimeCoefVariation(mt)
    } yield coef

  def moveTimes(pov: lila.game.Pov): Option[List[Centis]] =
    pov.game.moveTimes(pov.color)

  def highlyConsistentMoveTimes(pov: lila.game.Pov): Boolean =
    moveTimeCoefVariation(pov) ?? { cvIndicatesHighlyFlatTimes(_) }

  def cvIndicatesHighlyFlatTimes(c: Float) =
    c < 0.25

  def cvIndicatesHighlyFlatTimesForStreaks(c: Float) =
    c < 0.15

  def moderatelyConsistentMoveTimes(pov: lila.game.Pov): Boolean =
    moveTimeCoefVariation(pov) ?? { cvIndicatesModeratelyFlatTimes(_) }

  def cvIndicatesModeratelyFlatTimes(c: Float) =
    c < 0.4

  def slidingMoveTimesCvs(pov: lila.game.Pov): Option[Iterator[Float]] =
    moveTimes(pov) ?? { mt =>
      val onlyMid = mt.drop(7).dropRight(7)
      if (onlyMid.size < 10)
        mt.drop(7).dropRight(7).iterator.sliding(10).filter({ _.count(Centis(0)==) < 4 }).map({ a => moveTimeCoefVariationNoDrop(a.toList) }).flatten.some
      else None
    }

  def highlyConsistentMoveTimeStreaks(pov: lila.game.Pov): Boolean =
    slidingMoveTimesCvs(pov) ?? { mt =>
      mt.filter(cvIndicatesHighlyFlatTimesForStreaks(_)).nonEmpty
    }

  def moderatelyConsistentMoveTimeStreaks(pov: lila.game.Pov): Boolean =
    slidingMoveTimesCvs(pov) ?? { mt =>
      mt.filter(cvIndicatesModeratelyFlatTimes(_)).nonEmpty
    }

  private val fastMove = Centis(50)
  def noFastMoves(pov: lila.game.Pov): Boolean = {
    val moveTimes = ~pov.game.moveTimes(pov.color)
    moveTimes.count(fastMove >) <= (moveTimes.size / 20) + 2
  }

  def listAverage[T: Numeric](x: List[T]) = ~Maths.mean(x)

  def listDeviation[T: Numeric](x: List[T]) = ~Stats(x).stdDev
}