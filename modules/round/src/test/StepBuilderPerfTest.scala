package lila.round

import org.specs2.mutable.Specification

import chess._
import Pos._

class StepBuilderPerfTest extends Specification {

  sequential

  val nb = 200
  val gameMoves = Fixtures.prod500standard.take(nb).map {
    _.split(' ').toList
  }
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne(withOpening: Boolean)(moves: List[String]) =
    StepBuilder("abcd1234", moves, chess.variant.Standard, None, format.Forsyth.initial, withOpening = withOpening)
  def run(withOpening: Boolean) { gameMoves foreach runOne(withOpening) }

  def runTests(withOpening: Boolean) = {
      runOne(withOpening)(gameMoves.head)
      println("warming up")
      run(withOpening)
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run(withOpening)
        val duration = System.currentTimeMillis - start
        println(s"$nb games in $duration ms")
        duration
      }
      val nbGames = iterations * nb
      val moveMicros = (1000 * durations.sum) / nbGames
      println(s"Average = $moveMicros microseconds per game")
      println(s"          ${1000000 / moveMicros} games per second")
      true === true
  }

  "playing a game" should {
    "without opening" in {
      runTests(false)
    }
    "with opening" in {
      runTests(true)
    }
  }
}
