package shogi

import Pos._

class PlayPerfTest extends ShogiTest {

  args(skipAll = true)

  val nb         = 100
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne =
    makeGame.playMoves(
      C3 -> C4,
      B7 -> B6,
      C1 -> D2,
      G7 -> G6,
      D2 -> C3,
      C9 -> D8,
      H3 -> H4,
      G9 -> F8,
      G1 -> F2,
      F9 -> G8,
      D1 -> C2,
      E9 -> F9
    )
  def run: Unit = { for (i <- 1 to nb) runOne }

  "playing a game" should {
    "many times" in {
      runOne must beSuccess
      if (nb * iterations > 1) {
        println("warming up")
        run
      }
      println("running tests")
      val durations = for (i <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb games in $duration ms")
        duration
      }
      val nbGames    = iterations * nb
      val moveMicros = (1000 * durations.sum) / nbGames
      println(s"Average = $moveMicros microseconds per game")
      println(s"          ${1000000 / moveMicros} games per second")
      true === true
    }
  }
}
