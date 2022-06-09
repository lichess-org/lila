package lila.puzzle

import org.specs2.mutable.Specification

class PuzzleOpeningTest extends Specification {

  "nameToKey" in {
    import PuzzleOpening.{ Key, nameToKey }
    "opening name" in {
      nameToKey("Grünfeld Defense") must_== Key("Grunfeld_Defense")
      nameToKey("King's Pawn Game") must_== Key("Kings_Pawn_Game")
      nameToKey("Neo-Grünfeld Defense") must_== Key("Neo-Grunfeld_Defense")
    }
  }

}
