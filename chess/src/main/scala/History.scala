package lila.chess

import Pos.posAt

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

  def withoutAnyCastles = copy(
    whiteCastleKingSide = false,
    whiteCastleQueenSide = false,
    blackCastleKingSide = false,
    blackCastleQueenSide = false)

  def withoutCastle(color: Color, side: Side) = (color, side) match {
    case (White, KingSide)  ⇒ copy(whiteCastleKingSide = false)
    case (White, QueenSide) ⇒ copy(whiteCastleQueenSide = false)
    case (Black, KingSide)  ⇒ copy(blackCastleKingSide = false)
    case (Black, QueenSide) ⇒ copy(blackCastleQueenSide = false)
  }

  def withNewPositionHash(hash: String): History =
    copy(positionHashes = positionHashesWith(hash))

  def positionHashesWith(hash: String): List[String] =
    (hash take History.hashSize) :: positionHashes

  lazy val castleNotation: String = {
    (if (whiteCastleKingSide) "K" else "") +
      (if (whiteCastleQueenSide) "Q" else "") +
      (if (blackCastleKingSide) "k" else "") +
      (if (blackCastleQueenSide) "q" else "")
  } match {
    case "" ⇒ "-"
    case n  ⇒ n
  }

  def withLastMove(orig: Pos, dest: Pos) = copy(
    lastMove = Some((orig, dest))
  )
}

object History {

  val hashSize = 5

  val MoveString = """^([a-h][1-8]) ([a-h][1-8])$""".r

  def apply(
    lastMove: Option[String], // a2 a4
    positionHashes: String, // KQkq
    castles: String) = new History(
    lastMove = lastMove flatMap {
      case MoveString(a, b) ⇒ for (o ← posAt(a); d ← posAt(b)) yield (o, d)
      case _                ⇒ None
    },
    whiteCastleKingSide = castles contains 'K',
    whiteCastleQueenSide = castles contains 'Q',
    blackCastleKingSide = castles contains 'k',
    blackCastleQueenSide = castles contains 'q',
    positionHashes = positionHashes grouped hashSize toList)

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
