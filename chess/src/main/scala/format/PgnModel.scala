package lila.chess
package format

case class ParsedPgn(tags: List[Tag], sans: List[San])

sealed abstract class Tag(name: String, value: String) 

case class Fen(value: String) extends Tag("fen", value)

case class Unknown(name: String, value: String) extends Tag(name, value)

// Standard Algebraic Notation
sealed trait San {

  def apply(game: Game): Valid[Move]
}

case class Std(
    dest: Pos,
    role: Role,
    capture: Boolean = false,
    file: Option[Int] = None,
    rank: Option[Int] = None,
    check: Boolean = false,
    checkmate: Boolean = false,
    promotion: Option[PromotableRole] = None) extends San {

  def withSuffixes(s: Suffixes) = copy(
    check = s.check,
    checkmate = s.checkmate,
    promotion = s.promotion)

  def apply(game: Game): Valid[Move] = {
    def compare[A](a: Option[A], b: ⇒ A) = a map (_ == b) getOrElse true
    game.situation.moves map {
      case (orig, moves) ⇒ moves find { move ⇒
        move.dest == dest && move.piece.role == role
      }
    } collect {
      case Some(m) if compare(file, m.orig.x) && compare(rank, m.orig.y) ⇒ m
    } match {
      case Nil        ⇒ "No move found: %s\n%s".format(this, game.board).failNel
      case one :: Nil ⇒ success(one)
      case many       ⇒ "Many moves found: %s\n%s".format(many, game.board).failNel
    }
  }

}

case class Suffixes(
  check: Boolean,
  checkmate: Boolean,
  promotion: Option[PromotableRole])

case class Castle(side: Side) extends San {

  def apply(game: Game): Valid[Move] = for {
    kingPos ← game.board kingPosOf game.player toValid "No king found"
    actor ← game.board actorAt kingPos toValid "No actor found"
    move ← actor castleOn side toValid "Cannot castle"
  } yield move
}
