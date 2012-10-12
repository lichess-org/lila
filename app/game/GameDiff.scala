package lila
package game

final class GameDiff(a: RawDbGame, b: RawDbGame) {

  def apply(): List[(String, Any)] = {

    val builder = scala.collection.mutable.ListBuffer[(String, Any)]()

    def d[A](name: String, f: RawDbGame ⇒ A) {
      if (f(a) != f(b)) builder += name -> f(b)
    }

    d("s", _.s)
    d("t", _.t)
    d("lm", _.lm) // lastMove
    d("ck", _.ck) // check
    d("ph", _.ph) // positionHashes
    d("cs", _.cs) // castles
    d("lmt", _.lmt)
    for (i ← 0 to 1) {
      val name = "p." + i + "."
      d(name + "ps", _.p(i).ps) // pieces
      d(name + "w", _.p(i).w) // winner
      d(name + "lastDrawOffer", _.p(i).lastDrawOffer)
      d(name + "isOfferingDraw", _.p(i).isOfferingDraw)
      d(name + "isOfferingRematch", _.p(i).isOfferingRematch)
      d(name + "isProposingTakeback", _.p(i).isProposingTakeback)
      d(name + "bs", _.p(i).bs) // blurs
      d(name + "mts", _.p(i).mts) // movetimes
    }
    a.c foreach { c ⇒
      d("c.c", _.c.get.c)
      d("c.w", _.c.get.w)
      d("c.b", _.c.get.b)
      d("c.t", _.c.get.t) // timer
    }
    builder.toList
  }
}
