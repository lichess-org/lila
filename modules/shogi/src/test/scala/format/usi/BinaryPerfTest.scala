package shogi
package format
package usi

class BinaryPerfTest extends ShogiTest {

  val nb = 500
  val gameMoves = (format.usi.Fixtures.prod500standard take nb).map {
    Usi.readList(_).get
  }
  val iterations = 15

  def runOne(usis: List[Usi]) = {
    val bin = Binary.encodeMoves(variant.Standard, usis).toVector
    Binary.decodeMoves(variant.Standard, bin)
  }
  def run(): Unit = { gameMoves foreach runOne }

  "playing a game" should {
    "many times" in {
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
