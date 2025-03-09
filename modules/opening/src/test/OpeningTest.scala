package lila.opening

import chess.opening.OpeningName

class OpeningTest extends munit.FunSuite:
  import NameSection.variationName

  def vn(prev: String, next: String, expected: String) =
    assertEquals(variationName(OpeningName(prev), OpeningName(next)), NameSection(expected))

  test("actual progress (easy)"):
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

  test("change of family name"):
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

  test("change of variation"):
    vn("A:B", "A:Z", "Z")
    vn("A:B,C", "A:Y,Z", "Y")
    vn("A:B,C", "A:B,Z", "Z")
    vn("A:B,C,D", "A:Z", "Z")

  test("loss of variation"):
    vn("A:B", "A", "A")
    vn("A:B,C", "A:B", "B")
    vn("A:B,C,D", "A:B", "B")
    vn(
      "Semi-Slav Defense: Accelerated Move Order",
      "Semi-Slav Defense",
      "Semi-Slav Defense"
    )
