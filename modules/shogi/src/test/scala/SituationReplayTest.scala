package shogi

import format.usi.Usi

class SituationReplayTest extends ShogiTest {
  val usis = format.usi.Fixtures.prod500standard.map(Usi.readList(_).get)

  "all 500 fixtures" should {
    "have no errors and correct size" in {
      usis forall { u =>
        Replay.situations(u, None, shogi.variant.Standard) must beValid.like { case sits =>
          (sits.size - 1) must_== u.size
        }
      }
    }
  }

}
