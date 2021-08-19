package shogi

class HandTest extends ShogiTest {
  val hands = Hands.init

  "Empty hand" should {
    "have no pieces" in {
      hands.size must_== 0
    }
    "have 0 value" in {
      hands.value must_== 0
    }

    Role.handRoles foreach { r =>
      "have all pieces set to 0" in {
        (hands.sente(r) must_== 0) && (hands.gote(r) must_== 0)
      }
    }

    "have empty string export" in {
      hands.exportHands must_== "-"
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
        "have proper export" in {
          h.exportHands must_== "P"
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
        "have proper export" in {
          h.exportHands must_== "2P"
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
        "have proper export" in {
          h.exportHands must_== "2Pb"
        }
    }

  }
  "All roles" should {
    "pawn" in {
      Hands.init.store(Gote - Pawn).sente(Pawn) must_== 1
      Hands.init.store(Gote - Tokin).sente(Pawn) must_== 1
      Hands.init.store(Sente - Pawn).gote(Pawn) must_== 1
      Hands.init.store(Sente - Tokin).gote(Pawn) must_== 1

      Hands.init.store(Gote - Pawn).drop(Sente - Pawn).get must_== Hands.init
    }
    "lance" in {
      Hands.init.store(Gote - Lance).sente(Lance) must_== 1
      Hands.init.store(Gote - PromotedLance).sente(Lance) must_== 1
      Hands.init.store(Sente - Lance).gote(Lance) must_== 1
      Hands.init.store(Sente - PromotedLance).gote(Lance) must_== 1

      Hands.init.store(Gote - Lance).drop(Sente - Lance).get must_== Hands.init

    }
    "knight" in {
      Hands.init.store(Gote - Knight).sente(Knight) must_== 1
      Hands.init.store(Gote - PromotedKnight).sente(Knight) must_== 1
      Hands.init.store(Sente - Knight).gote(Knight) must_== 1
      Hands.init.store(Sente - PromotedKnight).gote(Knight) must_== 1

      Hands.init.store(Gote - Knight).drop(Sente - Knight).get must_== Hands.init
    }
    "silver" in {
      Hands.init.store(Gote - Silver).sente(Silver) must_== 1
      Hands.init.store(Gote - PromotedSilver).sente(Silver) must_== 1
      Hands.init.store(Sente - Silver).gote(Silver) must_== 1
      Hands.init.store(Sente - PromotedSilver).gote(Silver) must_== 1

      Hands.init.store(Gote - Silver).drop(Sente - Silver).get must_== Hands.init
    }
    "gold" in {
      Hands.init.store(Gote - Gold).sente(Gold) must_== 1
      Hands.init.store(Sente - Gold).gote(Gold) must_== 1

      Hands.init.store(Gote - Gold).drop(Sente - Gold).get must_== Hands.init
    }
    "bishop" in {
      Hands.init.store(Gote - Bishop).sente(Bishop) must_== 1
      Hands.init.store(Gote - Horse).sente(Bishop) must_== 1
      Hands.init.store(Sente - Bishop).gote(Bishop) must_== 1
      Hands.init.store(Sente - Horse).gote(Bishop) must_== 1

      Hands.init.store(Gote - Bishop).drop(Sente - Bishop).get must_== Hands.init
    }
    "rook" in {
      Hands.init.store(Gote - Rook).sente(Rook) must_== 1
      Hands.init.store(Gote - Dragon).sente(Rook) must_== 1
      Hands.init.store(Sente - Rook).gote(Rook) must_== 1
      Hands.init.store(Sente - Dragon).gote(Rook) must_== 1

      Hands.init.store(Gote - Rook).drop(Sente - Rook).get must_== Hands.init
    }
  }
}
