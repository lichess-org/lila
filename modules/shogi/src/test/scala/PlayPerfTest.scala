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
      (SQ7G, SQ7F, false),
      (SQ8C, SQ8D, false),
      (SQ7I, SQ6H, false),
      (SQ3C, SQ3D, false),
      (SQ6H, SQ7G, false),
      (SQ7A, SQ6B, false),
      (SQ2G, SQ2F, false),
      (SQ3A, SQ4B, false),
      (SQ3I, SQ4H, false),
      (SQ4A, SQ3B, false),
      (SQ6I, SQ7H, false),
      (SQ5A, SQ4A, false)
    )
  def run(): Unit = { for (_ <- 1 to nb) runOne }

  "playing a game" should {
    "many times" in {
      runOne must beValid
      if (nb * iterations > 1) {
        println("warming up")
        run()
      }
      println("running tests")
      val durations = for (_ <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run()
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
