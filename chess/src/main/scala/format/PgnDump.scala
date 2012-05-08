package lila.chess
package format

object PgnDump {

  def move(situation: Situation, data: Move, next: Situation): String = {
    import data._
    ((promotion, piece.role) match {
      case _ if castles   ⇒ if (orig ?> dest) "O-O-O" else "O-O"
      case _ if enpassant ⇒ orig.file + 'x' + dest.key
      case (promotion, Pawn) ⇒
        captures.fold(orig.file + "x", "") + 
        promotion.fold(p ⇒ dest.key + "=" + p.pgn, dest.key)
      case (_, role) ⇒ role.pgn + {
        val candidates = situation.actors filter { a ⇒
          a.piece.role == piece.role &&
            a.pos != orig &&
            (a.destinations contains dest)
        }
        if (candidates.isEmpty) ""
        else if (candidates exists (_.pos ?| orig))
          orig.file + orig.rank else orig.file
      } + captures.fold("x", "") + dest.key
      case _ ⇒ "?"
    }) + (if (next.check) if (next.checkMate) "#" else "+" else "")
  }
}
