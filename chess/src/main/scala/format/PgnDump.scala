package lila.chess
package format

object PgnDump {

  def move(situation: Situation, data: Move, nextSituation: Situation): String = {
    import data._
    ((promotion, piece.role) match {
      case _ if castle          ⇒ if (orig ?> dest) "O-O-O" else "O-O"
      case _ if enpassant       ⇒ orig.file + 'x' + dest.rank
      case (Some(promotion), _) ⇒ dest.key + promotion.pgn
      case (_, Pawn)            ⇒ (if (captures) orig.file + "x" else "") + dest.key
      case (_, role) ⇒ role.pgn + {
        val candidates = situation.actors filter { a ⇒
          a.piece.role == piece.role && a.pos != orig && (a.destinations contains dest)
        }
        if (candidates.isEmpty) ""
        else if (candidates exists (_.pos ?| orig)) orig.file + orig.rank else orig.file
      } + (if (captures) "x" else "") + dest.key
      case _ ⇒ "?"
    }) + (if (nextSituation.check) if (nextSituation.checkMate) "#" else "+" else "")
  }
}
