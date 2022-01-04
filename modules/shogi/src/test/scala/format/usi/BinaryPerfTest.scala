package shogi
package format
package usi

class BinaryPerfTest extends ShogiTest {

  //args(skipAll = true)

  val nb = 500
  val gameMoves = (format.usi.Fixtures.prod500standard take nb).map {
    Usi.readList(_).get
  }
  val iterations = 10
  // val nb = 1
  // val iterations = 1

  def runOne(usis: List[Usi]): Boolean = {
    val bin = Binary.encodeMoves(variant.Standard, usis).toList
    Binary.decodeMoves(variant.Standard, bin) must_== usis
  }
  def run: Boolean = { gameMoves forall runOne }

  "playing a game" should {
    "many times" in {
      run must beTrue
      println("running tests")
      val durations = for (_ <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
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
