package shogi

class ColorTest extends ShogiTest {

  "Color" should {
    "unary !" in {
      "sente" in { !Sente must_== Gote }
      "gote" in { !Gote must_== Sente }
    }
  }
}
