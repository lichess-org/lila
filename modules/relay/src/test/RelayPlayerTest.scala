package lila.relay

class RelayPlayerTest extends munit.FunSuite:

  test("dr. and prof."):
    assertEquals(RelayPlayerLine.tokenize("Pieper, Thomas Dr."), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Dr. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Prof. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayerLine.tokenize("Prof. Pieper Thomas Dr."), "pieper thomas")

  test("comma"):
    assertEquals(RelayPlayerLine.tokenize("Zimmer, Gerald"), "gerald zimmer")
    assertEquals(RelayPlayerLine.tokenize("Zimmer,Gerald"), "gerald zimmer")
