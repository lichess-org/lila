package shogi
package format
package usi

import cats.syntax.option._

class UsiMoveTest extends ShogiTest {

  "USI" in {
    Usi("9i1a") must beSome
    Usi("8h2b+") must beSome
    Usi("G*8b") must beSome
  }

  "piotr encoding" should {
    "be reflexive" in {
      val uciMove = Usi.Move("a2g7").get
      val usiMove = Usi.Move("8h3c").get
      Usi.Move piotr uciMove.piotr must_== uciMove.some
      Usi.Move piotr usiMove.piotr must_== usiMove.some
    }
  }
}
