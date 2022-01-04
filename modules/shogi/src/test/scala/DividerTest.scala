package shogi

class DividerTest extends ShogiTest {

  def makeReplay(moves: String) =
    format.Reader.fromUsi(shogi.format.usi.Usi.readList(moves).get, format.Tags.empty) match {
      case format.Reader.Result.Complete(replay) => replay.chronoMoves.map(_.fold(_.before, _.before))
      case x                                     => sys error s"Unexpected incomplete replay $x"
    }

  // more tests wanted
  "the divider finds middlegame and endgame" should {
    "game1" in {
      val replay = makeReplay(
        format.usi.Fixtures.fromProd2
      )
      val divided = Divider(replay)
      println("Game 1 => " + divided)
      divided.middle must beSome.like { case x =>
        x must beBetween(10, 30)
      }
      divided.end must beNone
    }
  }
}
