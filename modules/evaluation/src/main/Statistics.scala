package lila.evaluation

import chess.{ Centis, Stats }
import lila.common.Maths

object Statistics {

  case class IntAvgSd(avg: Int, sd: Int) {
    override def toString = s"$avg ± $sd"
    def /(div: Int)       = IntAvgSd(avg / div, sd / div)
  }

  def intAvgSd(values: List[Int]) = IntAvgSd(
    avg = listAverage(values).toInt,
    sd = listDeviation(values).toInt
  )

  // Coefficient of Variance
  def coefVariation(a: List[Int]): Option[Float] = {
    val s = Stats(a)
    s.stdDev.map { _ / s.mean }
  }

  // ups all values by 0.1s
  // as to avoid very high variation on bullet games
  // where all move times are low (https://lichess.org/@/AlisaP?mod)
  // and drops the first move because it's always 0
  def moveTimeCoefVariation(a: List[Centis]): Option[Float] =
    coefVariation(a.drop(1).map(_.centis + 10))

  def moveTimeCoefVariationNoDrop(a: List[Centis]): Option[Float] =
    coefVariation(a.map(_.centis + 10))

  def moveTimeCoefVariation(pov: lila.game.Pov): Option[Float] =
    for {
      mt   <- moveTimes(pov)
      coef <- moveTimeCoefVariation(mt)
    } yield coef

  def moveTimes(pov: lila.game.Pov): Option[List[Centis]] =
    pov.game.moveTimes(pov.color)

  def cvIndicatesHighlyFlatTimes(c: Float) =
    c < 0.25

  def cvIndicatesHighlyFlatTimesForStreaks(c: Float) =
    c < 0.14

  def cvIndicatesModeratelyFlatTimes(c: Float) =
    c < 0.4

  private val instantaneous = Centis(0)

  def slidingMoveTimesCvs(pov: lila.game.Pov): Option[Iterator[Float]] =
    moveTimes(pov) ?? {
      _.iterator
        .sliding(14)
        .map(_.toList.sorted.drop(1).dropRight(1))
        .filter(_.count(instantaneous ==) < 4)
        .flatMap(moveTimeCoefVariationNoDrop)
        .some
    }

  def moderatelyConsistentMoveTimes(pov: lila.game.Pov): Boolean =
    moveTimeCoefVariation(pov) ?? { cvIndicatesModeratelyFlatTimes(_) }

  private val fastMove = Centis(50)
  def noFastMoves(pov: lila.game.Pov): Boolean = {
    val moveTimes = ~pov.game.moveTimes(pov.color)
    moveTimes.count(fastMove >) <= (moveTimes.size / 20) + 2
  }

  def listAverage[T: Numeric](x: List[T]) = ~Maths.mean(x)

  def listDeviation[T: Numeric](x: List[T]) = ~Stats(x).stdDev
}
