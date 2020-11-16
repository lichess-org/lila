package chess

import org.specs2.mutable.Specification

class DecayingStatsTest extends Specification {

  import chess.{ DecayingStats => DS }

  val random = new java.util.Random(2286825201242408115L)

  def realMean(elts: Seq[Float]): Float = elts.sum / elts.size

  def realVar(elts: Seq[Float]): Float = {
    val mean = realMean(elts).toDouble
    (elts map { x =>
      Math.pow(x - mean, 2)
    } sum).toFloat / (elts.size - 1)
  }

  def beApprox(comp: Float) =
    (f: Float) => {
      comp must beCloseTo(f +/- 0.1f * comp)
    }

  "gaussian data" should {
    val randoms = Array.fill(1000) { random.nextGaussian.toFloat }
    val data10  = randoms map { _ + 10 }

    val stats10  = DS(10, 100, .9f).record(data10)
    val stats10d = DS(10, 100, 0.99f).record(data10)
    "eventually converge with constant mean" in {
      stats10.deviation must beCloseTo(1f +/- 0.25f)
      stats10.mean must beCloseTo(10f +/- 0.25f)

      stats10d.deviation must beCloseTo(1f +/- 0.25f)
      stats10d.mean must beCloseTo(10f +/- 0.1f)
    }

    "eventually converge with second mean" in {
      val stats2  = stats10.record(randoms)
      val stats2d = stats10d.record(randoms)

      stats2.deviation must beCloseTo(1f +/- 0.25f)
      stats2.mean must beCloseTo(0f +/- 0.25f)

      stats2d.deviation must beCloseTo(1f +/- 0.25f)
      stats2d.mean must beCloseTo(0f +/- 0.1f)
    }

    "quickly converge with new mean" in {
      stats10.record(randoms.take(20)).mean must beCloseTo(0f +/- 1f)
      // Not so quick with high decay...
      stats10d.record(randoms.take(100)).mean must beCloseTo(0f +/- 4f)
    }

    "converge with interleave" in {
      val dataI   = Array(data10, randoms).flatMap(_.zipWithIndex).sortBy(_._2).map(_._1)
      val statsIa = DS(10, 100, .9f).record(dataI)
      val statsIb = DS(10, 100, 0.99f).record(dataI)

      statsIa.deviation must beCloseTo(5f +/- 1f)
      statsIa.mean must beCloseTo(5f +/- 1f)

      statsIb.deviation must beCloseTo(5f +/- 0.25f)
      statsIb.mean must beCloseTo(5f +/- 0.25f)
    }
  }

  "flip flop data" should {
    val data  = Array.iterate(0f, 1000) { 1f - _ }
    val stats = DS(0, 10, .9f).record(data)
    "converge reasonably" in {
      stats.mean must beCloseTo(.5f +/- 0.05f)
      stats.deviation must beCloseTo(.5f +/- 0.05f)
    }
  }
}
