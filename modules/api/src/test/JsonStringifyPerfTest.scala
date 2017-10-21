package lila.api

import org.specs2.mutable.Specification
import play.api.libs.json._

class JsonStringifyPerfTest extends Specification {

  val jsonStr = """{"t":"had","d":{"id":"Xng0Mqgw","uid":"kxukwt3ngu","u":"Mihail_Shatohin","rating":1778,"ra":1,"clock":"5+0","t":300,"s":2,"c":"white","perf":"Blitz"}}"""
  val jsonObj = Json.parse(jsonStr)

  val nb = 100000
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def stringify = Json stringify jsonObj

  def runOne = stringify
  def run: Unit = { for (i ← 1 to nb) runOne }

  "stringify a hook" should {
    "many times" in {
      runOne must_== jsonStr
      if (nb * iterations > 1) {
        println("warming up")
        run
      }
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb runs in $duration ms")
        duration
      }
      val totalNb = iterations * nb
      val avg = (1000000 * durations.sum) / totalNb
      println(s"Average = $avg nanoseconds per run")
      println(s"          ${1000000000 / avg} runs per second")
      true === true
    }
  }
}
