package shogi
package format

class UciDumpPerfTest extends ShogiTest {

  args(skipAll = true)

  val nb         = 5
  val iterations = 10
  // val pgn = format.pgn.Fixtures.fromChessgames
  val pgn =
    "Pc4 Pg6 Pd4 Pd6 Sc2 Pd5 Sc3 Pd4 Sd4 Sd8 P*d5 Pc6 Pg4 Sd7 Sg2 P*d6 Gc2 Nc7 Pd6 Sd6 Gd3 P*d5 Sc3 Sg8 Sg3 Sg7 Sf4 Sf6 Ge4 Pe6 Sf5 Pe5 Ge5 Sf6e5 Sg6 Gg8 Re2 Sf6 Pe4 Sf6e7 P*d4 Pd4 Sd4 G*h6 Sf5 Pf6 Ng3 Pf5 Nf5 Sf6 Pe5 Sf5 Pe6 P*e8 Se5 Se5 Be5 Be5 Re5 Sg4 S*f2 S*b2 B*d6 N*d3 Kd2 Sa1+ Bc7+ Rd8 Hd8 Gd8 R*b9 Gd9 N*d7 Kd8 Pe7+ Pe7 Rb8+ Gc8 Da9 Ab1 De9 Kd7 Re7+"

  def runOne = UciDump(pgn.split(' ').toList, None, variant.Standard)
  def run: Unit = { for (i <- 1 to nb) runOne }

  "Dump UCI from PGN" should {
    "many times" in {
      // runOne must beSuccess
      if (nb * iterations > 1) {
        println("warming up")
        runOne
        run
      }
      println("running tests")
      val durations = for (i <- 1 to iterations) yield {
        val start = System.currentTimeMillis
        run
        val duration = System.currentTimeMillis - start
        println(s"$nb games in $duration ms")
        duration
      }
      val nbGames    = iterations * nb
      val gameMillis = durations.sum / nbGames
      println(s"Average = $gameMillis ms per game")
      println(s"          ${1000 / gameMillis} games per second")
      true === true
    }
  }
}
