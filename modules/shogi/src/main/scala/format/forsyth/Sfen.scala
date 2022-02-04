package shogi
package format
package forsyth

import cats.implicits._

import shogi.variant.Variant

final case class Sfen(value: String) extends AnyVal {

  def toSituationPlus(variant: Variant): Option[Sfen.SituationPlus] =
    toSituation(variant) map { sit =>
      val mn = moveNumber map (_ max 1 min 500)
      Sfen.SituationPlus(sit, mn | 1)
    }

  def toSituation(variant: Variant): Option[Situation] =
    for {
      board <- toBoard(variant)
      hands <- toHands(variant)
    } yield {
      val sit = Situation(board, hands, color | Sente, variant)
      if (color.isEmpty && sit.check) sit.switch else sit
    }

  def toBoard(variant: Variant): Option[Board] = {
    val positions = boardString | ""
    if (positions.count(_ == '/') == (variant.numberOfRanks - 1)) {
      Sfen.makePieceMapFromString(positions, variant) map Board.apply
    } else None
  }

  def toHands(variant: Variant): Option[Hands] =
    handsString.fold(Hands(variant).some)(Sfen.makeHandsFromString(_, variant))

  def boardString: Option[String] =
    value.split(' ').lift(0)

  def color: Option[Color] =
    value.split(' ').lift(1) flatMap (_.headOption) flatMap Color.apply

  def handsString: Option[String] =
    value.split(' ').lift(2)

  def moveNumber: Option[Int] =
    value.split(' ').lift(3) flatMap (_.toIntOption)

  def truncate = Sfen(value.split(' ') take 3 mkString " ")

  def initialOf(variant: Variant) = value == variant.initialSfen.value

  override def toString = value

}

object Sfen {

  def apply(game: Game): Sfen =
    apply(SituationPlus(game.situation, game.moveNumber))

  def apply(sp: SituationPlus): Sfen = 
    Sfen(s"${situationToString(sp.situation)} ${sp.moveNumber}")

  def apply(sit: Situation): Sfen =
    Sfen(s"${situationToString(sit)}")


  case class SituationPlus(situation: Situation, moveNumber: Int) {
    def plies = moveNumber - (if ((moveNumber % 2 == 1) == situation.color.sente) 1 else 0)
    def toSfen: Sfen = apply(this)
  }

  def situationToString(sit: Situation): String =
    List(
      boardToString(sit.board, sit.variant),
      sit.color.letter,
      handsToString(sit.hands, sit.variant)
    ) mkString " "

  def boardToString(board: Board, variant: Variant): String = {
    val sfen   = new scala.collection.mutable.StringBuilder(256)
    var empty = 0
    for (y <- 0 to (variant.numberOfRanks - 1)) {
      empty = 0
      for (x <- (variant.numberOfFiles - 1) to 0 by -1) {
        board(x, y) match {
          case None => empty = empty + 1
          case Some(piece) =>
            if (empty == 0) sfen append piece.forsyth
            else {
              sfen append (empty.toString + piece.forsyth)
              empty = 0
            }
        }
      }
      if (empty > 0) sfen append empty
      if (y < variant.numberOfRanks - 1) sfen append '/'
    }
    sfen.toString
  }

  private[forsyth] def handToString(hand: Hand, variant: Variant): String =
    variant.handRoles map { r =>
      val cnt = hand(r)
      if (cnt == 1) r.forsyth
      else if (cnt > 1) cnt.toString + r.forsyth
      else ""
    } mkString ""

  def handsToString(hands: Hands, variant: Variant): String =
    List(
      handToString(hands.sente, variant).toUpperCase,
      handToString(hands.gote, variant)
    ).mkString("").some.filterNot(_.isEmpty).getOrElse("-")

  private def makePieceMapFromString(boardStr: String, variant: Variant): Option[PieceMap] = {

    @scala.annotation.tailrec
    def piecesListRec(
      pieces: List[(Pos, Piece)],
      chars: List[Char],
      x: Int,
      y: Int
    ): Option[List[(Pos, Piece)]] =
      chars match {
        case Nil => Some(pieces)
        case '/' :: rest if y < variant.numberOfRanks => piecesListRec(pieces, rest, variant.numberOfFiles - 1, y + 1)
        case c :: rest if c.isDigit && x >= 0 => piecesListRec(pieces, rest, x - c.asDigit, y)
        case '+' :: c :: rest =>
          (for {
            pos <- Pos.at(x, y)
            _ <- Option.when(variant.isInsideBoard(pos))(())
            piece <- Piece.fromForsyth("+" + c)
          } yield (pos -> piece :: pieces)) match {
            case Some(ps) => piecesListRec(ps, rest, x - 1, y)
            case _ => None
          }
        case c :: rest => {
          (for {
            pos <- Pos.at(x, y)
            _ <- Option.when(variant.isInsideBoard(pos))(())
            piece <- Piece.fromForsyth(c.toString)
          } yield (pos -> piece :: pieces)) match {
            case Some(ps) => piecesListRec(ps, rest, x - 1, y)
            case _ => None
          }
        }
      }

    piecesListRec(Nil, boardStr.toList, variant.numberOfFiles - 1, 0) map (_.toMap)
  }

  def makeHandsFromString(handsStr: String, variant: Variant): Option[Hands] = {

    @scala.annotation.tailrec
    def handsRec(hands: Hands, chars: List[Char], curCount: Option[Int]): Option[Hands] =
      chars match {
        case Nil => Some(hands)
        case '-' :: _ => Some(Hands.empty)
        case d :: rest if d.isDigit =>
          handsRec(hands, rest, curCount.map(_ * 10 + d.asDigit) orElse d.asDigit.some)
        case p :: rest => Piece.fromForsyth(p.toString).filter(variant.handRoles contains _.role) match {
          case Some(piece) =>
            handsRec(hands.store(piece, curCount.fold(1)(math.min(_, 81))), rest, None)
          case _ => None
        }
      }
    
    handsRec(Hands.empty, handsStr.toList, None)
  }

  def clean(source: String): Sfen = Sfen(source.replace("_", " ").trim)

}
