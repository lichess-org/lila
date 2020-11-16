package chess

import org.specs2.mutable.Specification
import org.specs2.matcher.ValidationMatchers

class StatsTest extends Specification with ValidationMatchers {

  def realMean(elts: Seq[Float]): Float = elts.sum / elts.size

  def realVar(elts: Seq[Float]): Float = {
    val mean = realMean(elts).toDouble
    (elts map { x =>
      Math.pow(x - mean, 2)
    } sum).toFloat / (elts.size - 1)
  }

  def beApprox(comp: Float) =
    (f: Float) => {
      if (comp.isNaN) f.isNaN must beTrue
      else comp must beCloseTo(f +/- 0.001f * comp)
    }

  def beLike(comp: Stats) =
    (s: Stats) => {
      s.samples must_== comp.samples
      s.mean must beApprox(comp.mean)
      (s.variance, comp.variance) match {
        case (Some(sv), Some(cv)) => sv must beApprox(cv)
        case (sv, cv)             => sv must_== cv
      }
    }

  "empty stats" should {
    "have good defaults" in {
      Stats.empty.variance must_== None
      Stats.empty.mean must_== 0f
      Stats.empty.samples must_== 0
    }

    "convert to StatHolder" in {
      Stats(5) must beLike(StatHolder(0, 0f, 0f).record(5))

      "with good stats" in {
        Stats(5).samples must_== 1
        Stats(5).variance must_== None
        Stats(5).mean must_== 5f
      }
    }

  }

  "large values" should {
    // Tight data w/ large mean. Shuffled for Stats.
    val base         = (1 to 100) ++ (1 to 100) ++ (1 to 200)
    val data         = base map { _ + 1e5f }
    val shuffledData = base.sortWith(_ % 8 > _ % 8) map { _ + 1e5f }

    val statsN = Stats.empty record shuffledData
    "match actuals" in {
      statsN.mean must beApprox(realMean(data))
      statsN.variance.get must beApprox(realVar(data))
      statsN.samples must_== 400
    }
    "match concat" in {
      statsN must_== (Stats.empty + statsN)
      statsN must_== (statsN + Stats.empty)
      statsN must beLike(Stats(data take 1) + Stats(data drop 1))
      statsN must beLike(Stats(data take 100) + Stats(data drop 100))
      statsN must beLike(Stats(data take 200) + Stats(data drop 200))
    }
  }
}
