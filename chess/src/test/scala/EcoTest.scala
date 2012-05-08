package lila.chess


class EcoTest extends ChessTest {

  "Complete game" in {
    val game = "d4 Nf6 e4 Nxe4 f3 Nd6 g3"
    Eco nameOf game must beSome("Queen's Pawn Game")
  }

}
