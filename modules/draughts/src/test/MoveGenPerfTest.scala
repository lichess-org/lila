package draughts
package format

class MoveGenPerfTest extends DraughtsTest {

  //args(skipAll = true)

  val captureFen = "W:WK23:B28,17,10,20,29,39,40,38,31,8,9"
  val extremeFrisianFen = "W:WK50:B3,7,10,12,13,14,17,20,21,23,25,30,32,36,38,39,41,43,K47"

  "generate frisian moves" should {
    "once" in {
      val nb = 1
      val iterations = 3
      def runOne = Forsyth.<<@(draughts.variant.Frisian, extremeFrisianFen).get.validMoves
      def run(its: Int) { for (i ← 1 to its) runOne }
      println("warming up")
      runOne.size must_== 1
      runOne.head._2.size must_== 20
      runOne.head._2.head.capture.get.size must_== 19
      if (nb * iterations > 1) {
        run(nb * 4)
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

  /*"generate moves" should {
    "many times" in {
      val nb = 250
      val iterations = 30
      def runOne = (Forsyth << captureFen).get.validMoves
      def run(its: Int) { for (i ← 1 to its) runOne }
      runOne.size must_== 1
      runOne.head._2.size must_== 1
      runOne.head._2.head.capture.get.size must_== 10
      if (nb * iterations > 1) {
        println("warming up")
        run(nb * 20)
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
  }*/

}