package lila.opening

import org.specs2.mutable.Specification

class OpeningTest extends Specification {

  "next variation name" in {
    import Opening.variationName

    "actual progress (easy)" should {
      variationName("Polish Opening", "Polish Opening") must beNone
      variationName("Polish Opening", "Polish Opening: King's Indian Variation") must
        beSome("King's Indian Variation")
      variationName(
        "Polish Opening: King's Indian Variation",
        "Polish Opening: King's Indian Variation, Schiffler Attack"
      ) must beSome("Schiffler Attack")
    }

    "change of family name" should {
      variationName("Slav Defense", "Semi-Slav Defense: Accelerated Move Order") must
        beSome("Semi-Slav Defense: Accelerated Move Order")
      variationName("King's Knight Opening: Normal Variation", "Ruy Lopez") must beSome("Ruy Lopez")
    }

    "loss of variation" should {
      variationName("Semi-Slav Defense: Accelerated Move Order", "Semi-Slav Defense") must
        beSome("Semi-Slav Defense")
    }
  }
}
