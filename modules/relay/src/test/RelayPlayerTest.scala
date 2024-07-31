package lila.relay

class RelayPlayerTest extends munit.FunSuite:

  test("dr. and prof."):
    assertEquals(RelayPlayer.tokenize("Pieper, Thomas Dr."), "pieper thomas")
    assertEquals(RelayPlayer.tokenize("Dr. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayer.tokenize("Prof. Pieper Thomas"), "pieper thomas")
    assertEquals(RelayPlayer.tokenize("Prof. Pieper Thomas Dr."), "pieper thomas")
