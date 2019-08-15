package lila.rating

import org.joda.time.DateTime
import org.specs2.mutable.Specification

class GlickoTest extends Specification {

  "rating deviation" should {
    "provisional in 1 year" in {
      val now = new DateTime
      val glicko = Glicko(1500d, Glicko.minDeviation, 0.06d)
      val perf = Perf(glicko, 0, Nil, now.some)
      Glicko.system.previewDeviation(perf.toRating, now plusDays 365, false) must be_>=(Glicko.provisionalDeviation.toDouble)
    }
  }

}
