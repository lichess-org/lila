package lila.game

import org.specs2.mutable._

import shogi.format.usi._
import shogi.variant._

class PiecesPerfTest extends Specification {

  val nb         = 250
  val iterations = 10

  val usis        = Usi.readList(lila.game.Fixtures.fromProd3).get.toVector
  def runOne      = BinaryFormat.pieces.read(usis, None, Standard)
  def run(): Unit = { for (_ <- 1 to nb) runOne }

  "playing a game" should {
    "many times" in {
      if (nb * iterations > 1) {
        println("warming up")
        run()
      }
      println("running tests")
      val durations = for (_ <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run()
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
