package shogi
package opening

// format: off
object EcopeningDB {

  import Ecopening._

  val MAX_MOVES = 25

  lazy val all = allByEco.values.toList.sorted

  lazy val allByFen: Map[FEN, Ecopening] = allByEco.map {
    case (_, opening) => opening.fen -> opening
  }

  lazy val allByEco: Map[ECO, Ecopening] = Map(
"A00" -> new Ecopening("A00", "Uncommon Opening", "Uncommon Opening", "g4, a3, h3, etc.", "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR", ""),
"E99" -> new Ecopening("E99", "King's Indian", "King's Indian, Orthodox, Taimanov", "d4 Nf6 c4 g6 Nc3 Bg7 e4 d6 Nf3 O-O Be2 e5 O-O Nc6 d5 Ne7 Ne1 Nd7 f3 f5", "r1bq1rk1/pppnn1bp/3p2p1/3Ppp2/2P1P3/2N2P2/PP2B1PP/R1BQNRK1", "f7f5")
  )
}
