package shogi

class BerserkTest extends ShogiTest {

  def senteBerserk(minutes: Int, seconds: Int, byo: Int = 0) =
    Clock(minutes * 60, seconds, byo, 1).goBerserk(Sente).remainingTime(Sente).centis * .01

  "berserkable" should {
    "yep" in {
      Clock.Config(60 * 60, 0, 0, 1).berserkable must_== true
      Clock.Config(1 * 60, 0, 0, 1).berserkable must_== true
      Clock.Config(60 * 60, 60, 0, 1).berserkable must_== true
      Clock.Config(1 * 60, 0, 0, 1).berserkable must_== true
    }
    "nope" in {
      Clock.Config(0 * 60, 1, 0, 1).berserkable must_== false
      Clock.Config(0 * 60, 10, 0, 1).berserkable must_== false
    }
  }
  "berserk flags" should {
    "sente" in {
      Clock(60, 0, 0, 1).berserked(Sente) must_== false
      Clock(60, 0, 0, 1).goBerserk(Sente).berserked(Sente) must_== true
    }
    "gote" in {
      Clock(60, 0, 0, 1).berserked(Gote) must_== false
      Clock(60, 0, 0, 1).goBerserk(Gote).berserked(Gote) must_== true
    }
  }
  "initial time penalty, no increment" should {
    "10+0" in {
      senteBerserk(10, 0) must_== 5 * 60
    }
    "5+0" in {
      senteBerserk(5, 0) must_== 2.5 * 60
    }
    "3+0" in {
      senteBerserk(3, 0) must_== 1.5 * 60
    }
    "1+0" in {
      senteBerserk(1, 0) must_== 0.5 * 60
    }
  }
  "initial time penalty, with increment" should {
    "4+4" in {
      senteBerserk(4, 4) must_== 2 * 60
    }
    "3+2" in {
      senteBerserk(3, 2) must_== 1.5 * 60
    }
    "2+10" in {
      senteBerserk(2, 10) must_== 2 * 60
    }
    "10+5" in {
      senteBerserk(10, 5) must_== 5 * 60
    }
    "10+2" in {
      senteBerserk(10, 2) must_== 5 * 60
    }
    "1+1" in {
      senteBerserk(1, 1) must_== 0.5 * 60
    }
    "1+3" in {
      senteBerserk(1, 3) must_== 1 * 60
    }
    "1+5" in {
      senteBerserk(1, 5) must_== 1 * 60
    }
  }
}
