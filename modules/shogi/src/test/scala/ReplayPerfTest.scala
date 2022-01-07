package shogi

import format.usi._

class ReplayPerfTest extends ShogiTest {

  //args(skipAll = true)

  val nb = 100
  val gameUsis = (format.usi.Fixtures.prod500standard take nb).map {
    Usi.readList(_).get
  }
  val iterations = 5
  // val nb = 1
  // val iterations = 1

  def runOne(usis: List[Usi]): Boolean =
    Replay.gamesWhileValid(usis, None, shogi.variant.Standard)._1.tail.length == usis.length
  def run: Boolean = { gameUsis forall runOne }

  "playing a game" should {
    "many times" in {
      //runOne(gameMoves.head)._3 must beEmpty
      run must beTrue
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
