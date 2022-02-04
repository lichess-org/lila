package shogi

import format.usi.Usi

class ReplayTest extends ShogiTest {
  val usis = format.usi.Fixtures.prod500standard.map(Usi.readList(_).get)

  "all 500 fixtures" should {
    "have no errors and correct size" in {
      usis forall { u =>
        val r = Replay.gamesWhileValid(u, None, shogi.variant.Standard)
        r._1.tail.size must_== u.size
        r._2 must beEmpty
      }
    }
  }

}
