package lila.chess

class EloTest extends ChessTest {

  val calc = new EloCalculator
  import calc._

  def user(e: Int, n: Int) = new {
    val elo = e
    val nbGames = n
  }

  "calculate standard" should {
    "with equal elo" in {
      val (u1, u2) = (user(1400, 56), user(1400, 389))
      "p1 win" in {
        val (nu1, nu2) = calculate(u1, u2, -1)
        "new elos" in {
          (nu1, nu2) must_== (1416, 1384)
        }
        "conservation rule" in {
          nu1 - u1.elo + nu2 - u2.elo must_== 0
        }
      }
      "p1 loss" in {
        val (u1, u2) = (user(1400, 56), user(1400, 389))
        val (nu1, nu2) = calculate(u1, u2, 1)
        "new elos" in {
          (nu1, nu2) must_== (1384, 1416)
        }
        "conservation rule" in {
          nu1 - u1.elo + nu2 - u2.elo must_== 0
        }
      }
      "draw" in {
        val (u1, u2) = (user(1400, 56), user(1400, 389))
        val (nu1, nu2) = calculate(u1, u2, 0)
        "new elos" in {
          (nu1, nu2) must_== (1400, 1400)
        }
        "conservation rule" in {
          nu1 - u1.elo + nu2 - u2.elo must_== 0
        }
      }
    }
    "loss" in {
      val (u1, u2) = (user(1613, 56), user(1388, 389))
      val (nu1, nu2) = calculate(u1, u2, -1)
      "new elos" in {
        (nu1, nu2) must_== (1620, 1381)
      }
      "conservation rule" in {
        nu1 - u1.elo + nu2 - u2.elo must_== 0
      }
    }
  }
  "provision" should {
    val (u1, u2) = (user(1613, 8), user(1388, 389))
    val (nu1, nu2) = calculate(u1, u2, -1)
    "new elos" in {
      (nu1, nu2) must_== (1628, 1381)
    }
  }
  "no provision" should {
    val (u1, u2) = (user(1313, 1256), user(1158, 124))
    val (nu1, nu2) = calculate(u1, u2, -1)
    "new elos" in {
      (nu1, nu2) must_== (1322, 1149)
    }
  }
}
