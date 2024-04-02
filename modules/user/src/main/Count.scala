package lila.user

import lila.core.user.Count

object Count:

  import lila.db.dsl.*
  import lila.db.BSON
  import reactivemongo.api.bson.BSONDocumentHandler
  private[user] given BSONDocumentHandler[Count] = new BSON[Count]:

    def reads(r: BSON.Reader): Count =
      lila.core.user.Count(
        ai = r.nInt("ai"),
        draw = r.nInt("draw"),
        drawH = r.nInt("drawH"),
        game = r.nInt("game"),
        loss = r.nInt("loss"),
        lossH = r.nInt("lossH"),
        rated = r.nInt("rated"),
        win = r.nInt("win"),
        winH = r.nInt("winH")
      )

    def writes(w: BSON.Writer, o: Count) =
      $doc(
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

  val default = lila.core.user.Count(0, 0, 0, 0, 0, 0, 0, 0, 0)
