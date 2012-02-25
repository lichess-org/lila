package lila
package model

class SituationTest extends LilaSpec {

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
      }
    }
  }
}
