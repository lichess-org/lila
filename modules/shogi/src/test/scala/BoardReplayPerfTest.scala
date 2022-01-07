package shogi

import shogi.format.usi._

class BoardReplayPerfTest extends ShogiTest {

  //args(skipAll = true)

  val nb         = 100
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  val usis   = Usi.readList(format.usi.Fixtures.fromProd2).get
  def runOne = Replay.boards(usis, None, variant.Standard)
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
