package shogi
package format

import cats.syntax.option._

class UsiMoveTest extends ShogiTest {

  "piotr encoding" should {
    "be reflexive" in {
      val uciMove = Usi.Move("a2g7").get
      val usiMove = Usi.Move("8h3c").get
      Usi.Move piotr uciMove.piotr must_== uciMove.some
      Usi.Move piotr usiMove.piotr must_== usiMove.some
    }
  }
}
