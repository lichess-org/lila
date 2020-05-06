package lila.user

import lila.db.BSON
import reactivemongo.api.bson.BSONDocument

case class Count(
    ai: Int,
    draw: Int,
    drawH: Int, // only against human opponents
    game: Int,
    loss: Int,
    lossH: Int, // only against human opponents
    rated: Int,
    win: Int,
    winH: Int
) { // only against human opponents

  def gameH = winH + lossH + drawH

  def casual = game - rated
}

object Count {

  private[user] val countBSONHandler = new BSON[Count] {

    def reads(r: BSON.Reader): Count =
      Count(
        ai = r nInt "ai",
        draw = r nInt "draw",
        drawH = r nInt "drawH",
        game = r nInt "game",
        loss = r nInt "loss",
        lossH = r nInt "lossH",
        rated = r nInt "rated",
        win = r nInt "win",
        winH = r nInt "winH"
      )

    def writes(w: BSON.Writer, o: Count) =
      BSONDocument(
        "ai"    -> w.int(o.ai),
        "draw"  -> w.int(o.draw),
        "drawH" -> w.int(o.drawH),
        "game"  -> w.int(o.game),
        "loss"  -> w.int(o.loss),
        "lossH" -> w.int(o.lossH),
        "rated" -> w.int(o.rated),
        "win"   -> w.int(o.win),
        "winH"  -> w.int(o.winH)
      )
  }

  val default = Count(0, 0, 0, 0, 0, 0, 0, 0, 0)
}
