package lila.round

import org.specs2.mutable.Specification

import chess._

class StepBuilderPerfTest extends Specification {

  sequential

  val nb = 200
  val gameMoves = Fixtures.prod500standard.take(nb).map {
    _.split(' ').toVector
  }
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne(moves: Vector[String]) =
    StepBuilder("abcd1234", moves, chess.variant.Standard, format.Forsyth.initial)
  def run(): Unit = { gameMoves foreach runOne }

  def runTests() = {
    runOne(gameMoves.head)
    println("warming up")
    run()
    println("running tests")
    val durations = for (i ← 1 to iterations) yield {
      val start = System.currentTimeMillis
      run()
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
    "be fast" in {
      runTests()
    }
  }
}
