package chess

import Pos._

class PlayPerfTest extends ChessTest {

  args(skipAll = true)

  val nb         = 100
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne =
    makeGame.playMoves(
      E2 -> E4,
      D7 -> D5,
      E4 -> D5,
      D8 -> D5,
      B1 -> C3,
      D5 -> A5,
      D2 -> D4,
      C7 -> C6,
      G1 -> F3,
      C8 -> G4,
      C1 -> F4,
      E7 -> E6,
      H2 -> H3,
      G4 -> F3,
      D1 -> F3,
      F8 -> B4,
      F1 -> E2,
      B8 -> D7,
      A2 -> A3,
      E8 -> C8,
      A3 -> B4,
      A5 -> A1,
      E1 -> D2,
      A1 -> H1,
      F3 -> C6,
      B7 -> C6,
      E2 -> A6
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
