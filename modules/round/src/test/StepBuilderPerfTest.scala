package lila.round

import org.specs2.mutable.Specification

import chess._
import Pos._

class StepBuilderPerfTest extends Specification {

  val nb = 500
  val gameMoves = Fixtures.prod500standard.take(nb).map {
    _.split(' ').toList
  }
  val iterations = 5
  // val nb = 1
  // val iterations = 1

  def runOne(moves: List[String]) =
    StepBuilder("abcd1234", moves, chess.variant.Standard, None, None, true)
  def run { gameMoves foreach runOne }

  "playing a game" should {
    "many times" in {
      runOne(gameMoves.head)
      println("warming up")
      run
      println("running tests")
      val durations = for (i ← 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
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
  }
}
