package lila.common

import org.specs2.mutable.Specification

class ExecutionContextPerfTest extends Specification {

  sequential

  val nbRuns = 5

  def runTests(name: String, iterations: Int)(run: () => Any) = {
    println(s"$name warming up")
    for (i <- 1 to iterations) run()
    println(s"$name running")
    val durations = for (i ← 1 to nbRuns) yield {
      val start = System.currentTimeMillis
      for (i <- 1 to iterations) run()
      val duration = System.currentTimeMillis - start
      println(s"$name $iterations times in $duration ms")
      duration
    }
    val totalNb = iterations * nbRuns
    val moveNanos = (1000000 * durations.sum) / totalNb
    println(s"Average = $moveNanos nanoseconds each")
    println(s"          ${1000000000 / moveNanos} $name per second")
    true === true
  }

  val mapRange = 1 to 5000

  "map execontext" should {
    "default" in {
      runTests("default", 100) { () =>
        mapRange.foldLeft(fuccess(0)) {
          case (f, i) => f.map(_ => i)
        } awaitSeconds 10
      }
    }
    "direct" in {
      runTests("direct", 100) { () =>
        mapRange.foldLeft(fuccess(0)) {
          case (f, i) => f.map(_ => i)(lila.PimpedFuture.DirectExecutionContext)
        } awaitSeconds 10
      }
    }
  }
}
