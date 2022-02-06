package shogi

class DividerTest extends ShogiTest {

  val usis = shogi.format.usi.Usi.readList(format.usi.Fixtures.fromProd2).get

  // more tests wanted
  "the divider finds middlegame and endgame" should {
    "game1" in {
      val situations = Replay.situations(usis, None, variant.Standard).toOption.get
      val divided    = Divider(situations.toList)
      println("Game 1 => " + divided)
      divided.middle must beSome.like { case x =>
        x must beBetween(10, 30)
      }
      divided.end must beNone
    }
  }
}
