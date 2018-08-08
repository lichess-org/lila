package draughts
package format

class ForsythPerfTestBoardOut extends DraughtsTest {

  args(skipAll = true)

  val initialBoard = Board.init(variant.Standard)

  "export one position board" should {
    "many times" in {
      val nb = 10000
      val iterations = 15
      def runOne = Forsyth.exportBoard(initialBoard)
      def run { for (i ← 1 to nb) runOne }
      runOne must_== Forsyth.initialPieces
      if (nb * iterations > 1) {
        println("warming up")
        run
        run
      }
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb positions in $duration ms")
        duration
      }
      val nbPositions = iterations * nb
      val moveNanos = (1000000 * durations.sum) / nbPositions
      println(s"Average = $moveNanos nanoseconds per position")
      println(s"          ${1000000000 / moveNanos} positions per second")
      true === true
    }
  }

}

class ForsythPerfTestBoardIn extends DraughtsTest {

  args(skipAll = true)

  val initialFen = Forsyth.initial

  "create situation" should {
    "many times" in {
      val nb = 10000
      val iterations = 15
      def runOne = Forsyth <<< initialFen
      def run { for (i ← 1 to nb) runOne }
      runOne must_!= None
      if (nb * iterations > 1) {
        println("warming up")
        run
        run
      }
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb positions in $duration ms")
        duration
      }
      val nbPositions = iterations * nb
      val moveNanos = (1000000 * durations.sum) / nbPositions
      println(s"Average = $moveNanos nanoseconds per position")
      println(s"          ${1000000000 / moveNanos} positions per second")
      true === true
    }
  }

}