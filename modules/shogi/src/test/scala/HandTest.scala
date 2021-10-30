package shogi

class HandTest extends ShogiTest {
  val hands = Hands.init(shogi.variant.Standard)

  "Empty hand" should {
    "have no pieces" in {
      hands.size must_== 0
    }
    "have 0 value" in {
      hands.value must_== 0
    }

  }
  "Adding a gote pawn" should {
    hands.store(Gote - Pawn) match {
      case h =>
        "have 1 piece" in {
          h.size must_== 1
        }
        "have 1 sente pawn" in {
          h.sente(Pawn) must_== 1
        }
        "have proper value" in {
          h.value must_== 1
        }
    }
  }
  "Adding another gote pawn" should {
    hands
      .store(Gote - Pawn)
      .store(Gote - Pawn) match {
      case h =>
        "have 2 pieces" in {
          h.size must_== 2
        }
        "have 2 sente pawns" in {
          h.sente(Pawn) must_== 2
        }
        "have proper value" in {
          h.value must_== 2
        }
    }
  }
  "Adding sente bishop" should {
    hands
      .store(Gote - Pawn)
      .store(Gote - Pawn)
      .store(Sente - Bishop) match {
      case h =>
        "have 3 pieces" in {
          h.size must_== 3
        }
        "add to gote hand" in {
          h.gote(Bishop) must_== 1
        }
        "have proper value" in {
          h.value must_== -6
        }
    }

  }
}
