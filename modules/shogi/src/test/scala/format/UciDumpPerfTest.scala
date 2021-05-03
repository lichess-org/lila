package chess
package format

class UciDumpPerfTest extends ChessTest {

  args(skipAll = true)

  val nb         = 5
  val iterations = 10
  // val pgn = format.pgn.Fixtures.fromChessgames
  val pgn =
    "Nc3 Nc6 Nf3 Nf6 d4 d6 e4 Be6 a3 Qd7 d5 Bg4 dxc6 Qxc6 Bb5 Qxb5 Nxb5 Kd8 h3 Bd7 a4 Nxe4 Qd4 Bxb5 axb5 Nc5 b4 Ne6 Qd5 Kc8 b6 cxb6 b5 Kc7 Qc4+ Kd8 Ke2 Rc8 Qa4 Ke8 Ra2 Ra8 Be3 Nc5 Qb4 e6 Rd1 d5 Ne5 Nd3 cxd3 Bxb4 Rc1 Bd6 f4 f6 Ng4 Kf7 Bd4 Bxf4 Rf1 e5 Bxe5 fxe5 Nxe5+ Ke6 d4 Bxe5 dxe5 Kxe5 Rd2 Rhf8 Rfd1 Rad8 Ke3 a6 Rd4 axb5 Kd3 Rf2 Re1+ Kf6 Ke3 Rxg2 Kf3 Rc2 Rf4+ Kg5 Rg4+ Kf6"

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
