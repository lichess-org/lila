package chess

class ReplayPerfTest extends ChessTest {

  args(skipAll = true)

  val nb = 500
  val gameMoves = (format.pgn.Fixtures.prod500standard take nb).map {
    _.split(' ').toList
  }
  val iterations = 5
  // val nb = 1
  // val iterations = 1

  def runOne(moves: List[String]) =
    Replay.gameMoveWhileValid(moves, format.Forsyth.initial, chess.variant.Standard)
  def run: Unit = { gameMoves foreach runOne }

  "playing a game" should {
    "many times" in {
      runOne(gameMoves.head)._3 must beEmpty
      run
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
