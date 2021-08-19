package shogi
package format

class ForsythPerfTest extends ShogiTest {

  args(skipAll = true)

  val initialBoard = Board.init(variant.Standard)
  val emptyBoard   = (Forsyth << "9/9/9/9/9/9/9/9/9").get.board

  "export one position board" should {
    "many times" in {
      val nb         = 10
      val iterations = 10
      def runOne     = Forsyth.exportBoard(initialBoard)
      def run { for (i <- 1 to nb) runOne }
      runOne must_== Forsyth.initial.takeWhile(' ' !=)
      if (nb * iterations > 1) {
        println("warming up")
        run
      }
      println("running tests")
      val durations = for (i <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb positions in $duration ms")
        duration
      }
      val nbPositions = iterations * nb
      val moveNanos   = (1000000 * durations.sum) / nbPositions
      println(s"Average = $moveNanos nanoseconds per position")
      println(s"          ${1000000000 / moveNanos} positions per second")
      true === true
    }
  }
}
