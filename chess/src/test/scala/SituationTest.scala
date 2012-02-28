package lila.chess

class SituationTest extends LilaTest {

  "a game" should {
    "detect" in {
      "check" in {
        "by rook" in {
          ("""
K  r
""" as White).check must beTrue
        }
        "by knight" in {
          ("""
  n
K
""" as White).check must beTrue
        }
        "not" in {
          ("""
   n
K
""" as White).check must beFalse
        }
      }
      "check mate" in {
        "by rook" in {
          ("""
PP
K  r
""" as White).checkMate must beTrue
        }
        "by knight" in {
          ("""
PPn
KR
""" as White).checkMate must beTrue
        }
        "not" in {
          ("""
  n
K
""" as White).checkMate must beFalse
        }
      }
      "stale mate" in {
        "stuck in a corner" in {
          ("""
prr
K
""" as White).staleMate must beTrue
        }
        "not" in {
          ("""
  b
K
""" as White).staleMate must beFalse
        }
      }
    }
  }
}
