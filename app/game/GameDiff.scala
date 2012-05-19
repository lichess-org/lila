package lila
package game

final class GameDiff(a: RawDbGame, b: RawDbGame) {

  def apply(): List[(String, Any)] = {

    val builder = scala.collection.mutable.ListBuffer[(String, Any)]()

    def d[A](name: String, f: RawDbGame ⇒ A) {
      if (f(a) != f(b)) builder += name -> f(b)
    }

    d("pgn", _.pgn)
    d("status", _.status)
    d("turns", _.turns)
    d("lastMove", _.lastMove)
    d("check", _.check)
    d("positionHashes", _.positionHashes)
    d("castles", _.castles)
    d("lmt", _.lmt)
    for (i ← 0 to 1) {
      val name = "players." + i + "."
      d(name + "ps", _.players(i).ps)
      d(name + "w", _.players(i).w)
      d(name + "lastDrawOffer", _.players(i).lastDrawOffer)
      d(name + "isOfferingDraw", _.players(i).isOfferingDraw)
      d(name + "isOfferingRematch", _.players(i).isOfferingRematch)
      d(name + "isProposingTakeback", _.players(i).isProposingTakeback)
      d(name + "blurs", _.players(i).blurs)
      d(name + "mts", _.players(i).mts)
    }
    a.clock foreach { c ⇒
      d("clock.c", _.clock.get.c)
      d("clock.w", _.clock.get.w)
      d("clock.b", _.clock.get.b)
      d("clock.timer", _.clock.get.timer)
    }
    builder.toList
  }
}
