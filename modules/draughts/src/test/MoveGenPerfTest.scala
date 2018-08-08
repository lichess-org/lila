package draughts
package format

class MoveGenPerfTest extends DraughtsTest {

  //args(skipAll = true)

  val captureFen = "W:WK23:B28,17,10,20,29,39,40,38,31,8,9"

  "generate moves" should {
    "many times" in {
      val nb = 500
      val iterations = 15
      def runOne = (Forsyth << captureFen).get.validMoves
      def run(its: Int) { for (i ← 1 to its) runOne }
      runOne.size must_== 1
      runOne.head._2.size must_== 1
      runOne.head._2.head.capture.get.size must_== 10
      if (nb * iterations > 1) {
        println("warming up")
        run(nb * 3)
      }
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run(nb)
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