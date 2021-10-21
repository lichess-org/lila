package shogi

import Pos._

class PlayPerfTest extends ShogiTest {

  //args(skipAll = true)

  val nb         = 100
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne =
    makeGame.playMoves(
      SQ7G -> SQ7F,
      SQ8C -> SQ8D,
      SQ7I -> SQ6H,
      SQ3C -> SQ3D,
      SQ6H -> SQ7G,
      SQ7A -> SQ6B,
      SQ2G -> SQ2F,
      SQ3A -> SQ4B,
      SQ3I -> SQ4H,
      SQ4A -> SQ3B,
      SQ6I -> SQ7H,
      SQ5A -> SQ4A
    )
  def run: Unit = { for (_ <- 1 to nb) runOne }

  "playing a game" should {
    "many times" in {
      runOne must beValid
      if (nb * iterations > 1) {
        println("warming up")
        run
      }
      println("running tests")
      val durations = for (_ <- 1 to iterations) yield {
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
