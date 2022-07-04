package lila.puzzle

import org.specs2.mutable.Specification

class PuzzleOpeningTest extends Specification {

  import lila.common.LilaOpening.nameToKey

  "nameToKey" in {
    "opening name" in {
      nameToKey("Grünfeld Defense") must_== "Grunfeld_Defense"
      nameToKey("King's Pawn Game") must_== "Kings_Pawn_Game"
      nameToKey("Neo-Grünfeld Defense") must_== "Neo-Grunfeld_Defense"
    }
  }

}
