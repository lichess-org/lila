package lila.opening

import org.specs2.mutable.Specification

class OpeningTest extends Specification {

  "next variation name" in {
    import Opening.variationName

    def vn(prev: String, next: String, expected: String) =
      variationName(prev, next) must_== expected

    "actual progress (easy)" in {
      vn("A", "A", "A")
      vn("A", "A: B", "B")
      vn("A", "A: B, C", "B")
      vn("A: B", "A: B, C", "C")
      vn("", "A", "A")
      vn("", "A:B", "A")
      vn("", "A:B,C,D", "A")
      vn("Polish Opening", "Polish Opening", "Polish Opening")
      vn(
        "Polish Opening",
        "Polish Opening: King's Indian Variation",
        "King's Indian Variation"
      )
      vn(
        "Polish Opening: King's Indian Variation",
        "Polish Opening: King's Indian Variation, Schiffler Attack",
        "Schiffler Attack"
      )
    }

    "change of family name" in {
      vn("A", "B", "B")
      vn("A:B", "Z", "Z")
      vn("A:B", "Z:B", "Z")
      vn("A:B,C", "Z:B", "Z")
      vn(
        "Slav Defense",
        "Semi-Slav Defense: Accelerated Move Order",
        "Semi-Slav Defense"
      )
      vn("King's Knight Opening: Normal Variation", "Ruy Lopez", "Ruy Lopez")
    }

    "change of variation" in {
      vn("A:B", "A:Z", "Z")
      vn("A:B,C", "A:Y,Z", "Y")
      vn("A:B,C", "A:B,Z", "Z")
      vn("A:B,C,D", "A:Z", "Z")
    }

    "loss of variation" in {
      vn("A:B", "A", "A")
      vn("A:B,C", "A:B", "B")
      vn("A:B,C,D", "A:B", "B")
      vn(
        "Semi-Slav Defense: Accelerated Move Order",
        "Semi-Slav Defense",
        "Semi-Slav Defense"
      )
    }
  }
}
