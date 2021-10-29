package shogi
package format

import cats.syntax.option._

class UciMoveTest extends ShogiTest {

  "piotr encoding" should {
    "be reflexive" in {
      val uciMove = Uci.Move("a2g7").get
      val usiMove = Uci.Move("8h3c").get
      Uci.Move piotr uciMove.piotr must_== uciMove.some
      Uci.Move piotr usiMove.piotr must_== usiMove.some
    }
  }
}
