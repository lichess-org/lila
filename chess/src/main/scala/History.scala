package lila.chess

case class History(
    lastMove: Option[(Pos, Pos)] = None,
    positionHashes: List[String] = Nil,
    whiteCastleKingSide: Boolean = true,
    whiteCastleQueenSide: Boolean = true,
    blackCastleKingSide: Boolean = true,
    blackCastleQueenSide: Boolean = true) {

  def isLastMove(p1: Pos, p2: Pos): Boolean = lastMove == (p1, p2)

  def threefoldRepetition: Boolean = positionHashes.size > 6 && {
    positionHashes.headOption map { hash ⇒
      positionHashes.count(_ == hash) >= 3
    } getOrElse false
  }

  def canCastle(color: Color) = new {
    def on(side: Side): Boolean = (color, side) match {
      case (White, KingSide)  ⇒ whiteCastleKingSide
      case (White, QueenSide) ⇒ whiteCastleQueenSide
      case (Black, KingSide)  ⇒ blackCastleKingSide
      case (Black, QueenSide) ⇒ blackCastleQueenSide
    }
    def any = on(KingSide) || on(QueenSide)
  }

  def withoutCastles(color: Color) = color match {
    case White ⇒ copy(
      whiteCastleKingSide = false,
      whiteCastleQueenSide = false)
    case Black ⇒ copy(
      blackCastleKingSide = false,
      blackCastleQueenSide = false)
  }

  def withoutCastle(color: Color, side: Side) = (color, side) match {
    case (White, KingSide)  ⇒ copy(whiteCastleKingSide = false)
    case (White, QueenSide) ⇒ copy(whiteCastleQueenSide = false)
    case (Black, KingSide)  ⇒ copy(blackCastleKingSide = false)
    case (Black, QueenSide) ⇒ copy(blackCastleQueenSide = false)
  }

  def withoutPositionHashes: History = copy(positionHashes = Nil)

  def withNewPositionHash(hash: String): History =
    copy(positionHashes = (hash take History.hashSize) :: positionHashes)

  def castleNotation: String =
    (if (whiteCastleKingSide) "K" else "") +
    (if (whiteCastleQueenSide) "Q" else "") +
    (if (blackCastleKingSide) "k" else "") +
    (if (blackCastleQueenSide) "q" else "")
}

object History {

  val hashSize = 5

  def castle(color: Color, kingSide: Boolean, queenSide: Boolean) = color match {
    case White ⇒ History().copy(
      whiteCastleKingSide = kingSide,
      whiteCastleQueenSide = queenSide)
    case Black ⇒ History().copy(
      blackCastleKingSide = kingSide,
      blackCastleQueenSide = queenSide)
  }

  def noCastle = History() withoutCastles White withoutCastles Black
}
