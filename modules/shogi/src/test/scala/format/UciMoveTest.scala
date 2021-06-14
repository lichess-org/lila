package shogi
package format

class UciMoveTest extends ShogiTest {

  "piotr encoding" should {
    "be reflexive" in {
      val move = Uci.Move("a2g7").get
      Uci.Move piotr move.piotr must_== move.some
    }
  }
}
