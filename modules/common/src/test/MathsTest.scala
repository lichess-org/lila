package lila.common
import org.specs2.mutable.Specification

class MathsTest extends Specification {

  import lila.common.Maths._

  "standard deviation" should {
    "empty collection" in {
      standardDeviation(Nil) must beNone
    }
    "single value" in {
      standardDeviation(List(3)) must beSome(0)
    }
    "list" in {
      // https://www.scribbr.com/statistics/standard-deviation/
      // ~standardDeviation(List(46, 69, 32, 60, 52, 41)) must beCloseTo(13.31, 0.01) // sample
      ~standardDeviation(List(46, 69, 32, 60, 52, 41)) must beCloseTo(12.15, 0.01) // population
    }
  }
}
