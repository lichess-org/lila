package shogi
package format
package usi

class BinaryPerfTest extends ShogiTest {

  val usis = format.usi.Fixtures.prod500standard.map(Usi.readList(_).get)
  val iterations = 15

  def runOne(usis: List[Usi]) =
    Binary.decodeMoves(Binary.encodeMoves(usis, variant.Standard).toVector, variant.Standard)
  def run(): Unit = { usis foreach runOne }

  "playing a game" should {
    "many times" in {
      println("warming up")
      run()
      println("running tests")
      val durations = for (_ <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run()
        val duration = System.currentTimeMillis - start
        println(s"${usis.size} games in $duration ms")
        duration
      }
      val nbGames    = iterations * usis.size
      val moveMicros = (1000 * durations.sum) / nbGames
      println(s"Average = $moveMicros microseconds per game")
      println(s"          ${1000000 / moveMicros} games per second")
      true === true
    }
  }
}
