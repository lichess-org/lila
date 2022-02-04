package shogi

class HandTest extends ShogiTest {
  val hands = Hands.empty

  "Empty hand" should {
    "have no pieces" in {
      hands.size must_== 0
      hands.isEmpty must beTrue
      hands.sente.isEmpty must beTrue
      hands.gote.isEmpty must beTrue
    }
    "have 0 value" in {
      hands.roleValue must_== 0
    }

  }
  "Adding a sente pawn" should {
    hands.store(Sente - Pawn) match {
      case h =>
        "have 1 piece" in {
          h.size must_== 1
        }
        "have 1 sente pawn" in {
          h.sente(Pawn) must_== 1
        }
        "have proper value" in {
          h.roleValue must_== 1
        }
        "not be empty" in {
          h.isEmpty must beFalse
          h.nonEmpty must beTrue
          h.sente.isEmpty must beFalse
          h.sente.nonEmpty must beTrue
        }
        "gote still empty" in {
          h.gote.isEmpty must beTrue
          h.gote.nonEmpty must beFalse
        }
    }
  }
  "Taking the piece away" should {
    hands.store(Sente - Pawn).take(Sente - Pawn) match {
      case Some(h) =>
        "have no pieces" in {
          h.size must_== 0
          h.isEmpty must beTrue
          hands.sente.isEmpty must beTrue
          hands.gote.isEmpty must beTrue
        }
    }
  }
  "Adding more sente pawns" should {
    hands
      .store(Sente - Pawn)
      .store(Sente - Pawn) match {
      case h =>
        "have 2 pieces" in {
          h.size must_== 2
        }
        "have 2 sente pawns" in {
          h.sente(Pawn) must_== 2
        }
        "have proper value" in {
          h.roleValue must_== 2
        }
    }
  }
  "Taking another pawn" should {
    hands
      .store(Sente - Pawn)
      .store(Sente - Pawn)
      .take(Sente - Pawn) match {
      case Some(h) =>
        "have 1 pieces" in {
          h.size must_== 1
        }
        "have 1 sente pawn" in {
          h.sente(Pawn) must_== 1
        }
    }
  }
  "Adding gote bishop" should {
    hands
      .store(Sente - Pawn)
      .store(Sente - Pawn)
      .store(Gote - Bishop) match {
      case h =>
        "have 3 pieces" in {
          h.size must_== 3
        }
        "add to gote hand" in {
          h.gote(Bishop) must_== 1
        }
        "have proper value" in {
          h.roleValue must_== -6
        }
    }

  }
}
