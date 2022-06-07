package lila.puzzle

import org.specs2.mutable.Specification

class PuzzleOpeningTest extends Specification {

  "slugify" in {
    import PuzzleOpening.slugify
    "opening name" in {
      slugify("Grünfeld Defense") must_== "Grunfeld_Defense"
      slugify("King's Pawn Game") must_== "Kings_Pawn_Game"
      slugify("Neo-Grünfeld Defense") must_== "Neo-Grunfeld_Defense"
    }
  }

}
