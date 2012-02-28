package lila.chess
package format

object PgnDump {

  def move(m: Move): String = {
    import m._

    def disambiguate = {
      val candidates = situation.actors filter { a ⇒
        a.pos != orig && a.piece.role == piece.role && (a.destinations contains dest)
      }
      if (candidates.isEmpty) ""
      else if (candidates exists (_.pos ?| orig)) orig.file + orig.rank else orig.file
    }
    def capturing = if (captures) "x" else ""
    def checking = if (checkMates) "#" else if (checks) "+" else ""

    ((promotion, piece.role) match {
      case _ if castle           ⇒ if (orig ?> dest) "O-O-O" else "O-O"
      case _ if enpassant        ⇒ orig.file + 'x' + dest.rank
      case (Some(promotion), _)  ⇒ dest.key + promotion.pgn
      case (_, Pawn) if captures ⇒ orig.file + 'x' + dest.key
      case (_, Pawn)             ⇒ dest.key
      case (_, role)             ⇒ role.pgn + disambiguate + capturing + dest.key
      case _                     ⇒ "?"
    }) + checking
  }
}
