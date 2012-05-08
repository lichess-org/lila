package lila.chess

class EcoTest extends ChessTest {

  def name(g: String) = Eco openingOf g map (_.name)

  val g1 = "d4 Nf6 e4 Nxe4 f3 Nd6 g3" 
  g1 in {
    name(g1) must_== Some("Queen's Pawn Game")
  }
  val g2 = "e4 e5 Nf3 Nf6" 
  g2 in {
    name(g2) must_== Some("Petrov Defense")
  }
  val g3 = "e4 e5" 
  g3 in {
    name(g3) must_== Some("King's Pawn Game")
  }
  val g4 = "e4 e5 b3 Nc6" 
  g4 in {
    name(g4) must_== Some("King's Pawn Game")
  }
  val g5 = "e4 e5 b3 Nc6 Nc3"
  g5 in {
    name(g5) must_== Some("King's Pawn Game")
  }
}
