package shogi
package variant

import shogi.Pos._

case object Horde
    extends Variant(
      id = 8,
      key = "horde",
      name = "Horde",
      shortName = "Horde",
      title = "Destroy the horde to win!",
      standardInitialPosition = false
    ) {

  /** In Horde chess sente advances against gote with a horde of pawns.
    */
  lazy val pieces: Map[Pos, Piece] = {

    val frontPawns = List(Pos.B5, Pos.C5, Pos.F5, Pos.G5).map { _ -> Sente.pawn }

    val sentePawnsHorde = frontPawns ++ (for {
      x <- 1 to 8
      y <- 1 to 4
    } yield Pos.posAt(x, y) map (_ -> Sente.pawn)).flatten toMap

    val gotePieces = (for (y <- 7 to 8; x <- 1 to 8) yield {
      posAt(x, y) map { pos =>
        (
          pos,
          y match {
            case 8 => Gote - backRank(x - 1)
            case 7 => Gote.pawn
          }
        )
      }
    }).flatten.toMap

    gotePieces ++ sentePawnsHorde
  }

  override val initialFen = "rnbqkbnr/pppppppp/8/1PP2PP1/PPPPPPPP/PPPPPPPP/PPPPPPPP/PPPPPPPP w kq - 0 1"

  override def valid(board: Board, strict: Boolean) =
    board.kingPosOf(Sente).isEmpty && validSide(board, strict)(Gote)

  /** The game has a special end condition when sente manages to capture all of gote's pawns */
  override def specialEnd(situation: Situation) =
    situation.board.piecesOf(Sente).isEmpty

  /** Any vs K + any where horde is stalemated and only king can move is a fortress draw
    * This does not consider imminent fortresses such as 8/p7/P7/8/8/P7/8/k7 b - -
    * nor does it consider contrived fortresses such as b7/pk6/P7/P7/8/8/8/8 b - -
    */
  private def hordeClosedPosition(board: Board) = {
    lazy val notKingBoard = board.kingPos.get(Color.gote).flatMap(board.take).getOrElse(board)
    val hordePos          = board.occupation(Color.sente) // may include promoted pieces
    val mateInOne =
      hordePos.size == 1 && hordePos.forall(pos => pieceThreatened(board, Color.gote, pos, (_ => true)))
    !mateInOne && notKingBoard.actors.values.forall(actor => actor.moves.isEmpty)
  }

  /** In horde chess, gote can win unless a fortress stalemate is unavoidable.
    *  Auto-drawing the game should almost never happen, but it did in https://lishogi.org/xQ2RsU8N#121
    */
  override def isInsufficientMaterial(board: Board) = hordeClosedPosition(board)

  /** In horde chess, the horde cannot win on * V K or [BN]{2} v K or just one piece
    * since they lack a king for checkmate support.
    * Technically there are some positions where stalemate is unavoidable which
    * this method does not detect; however, such are trivial to premove.
    */
  override def opponentHasInsufficientMaterial(situation: Situation): Boolean = {
    val board         = situation.board
    val opponentColor = !situation.color
    lazy val fortress = hordeClosedPosition(board) // costly function call
    if (opponentColor == Color.sente) {
      lazy val notKingPieces           = InsufficientMatingMaterial.nonKingPieces(board) toList
      val horde                        = board.piecesOf(Color.sente)
      lazy val hordeBishopSquareColors = horde.filter(_._2.is(Bishop)).toList.map(_._1.color).distinct
      lazy val hordeRoles              = horde.map(_._2.role)
      lazy val army                    = board.piecesOf(Color.gote)
      lazy val armyPawnsOrRooks        = army.filter(p => p._2.is(Pawn) || p._2.is(Rook))
      lazy val armyPawnsOrBishops      = army.filter(p => p._2.is(Pawn) || p._2.is(Bishop))
      lazy val armyPawnsOrKnights      = army.filter(p => p._2.is(Pawn) || p._2.is(Knight))
      lazy val armyNonQueens           = army.filter(_._2.isNot(Lance))
      lazy val armyNonQueensOrRooks    = army.filter(p => p._2.isNot(Lance) && p._2.isNot(Rook))
      lazy val armyNonQueensOrBishops  = army.filter(p => p._2.isNot(Lance) && p._2.isNot(Bishop))
      lazy val armyBishopSquareColors  = army.filter(_._2.is(Bishop)).toList.map(_._1.color).distinct
      if (horde.size == 1) {
        hordeRoles match {
          case List(Knight) =>
            army.size < 4 || armyNonQueensOrRooks.isEmpty || armyNonQueensOrBishops.isEmpty || (armyNonQueensOrBishops.size + armyBishopSquareColors.size) < 4
          case List(Bishop) =>
            notKingPieces.count(p =>
              p._2.is(Pawn) || (p._2.is(Bishop) && p._1.color != horde.head._1.color)
            ) < 2
          case List(Rook) => army.size < 3 || armyPawnsOrRooks.isEmpty || armyPawnsOrKnights.isEmpty
          case _          => armyPawnsOrRooks.isEmpty
        }
      } else if (
        (hordeRoles.forall(
          _ == Bishop
        ) && hordeBishopSquareColors.size == 1) && (armyPawnsOrKnights.size + armyPawnsOrBishops
          .count(p => p._1.color != horde.head._1.color) < 2)
      ) true
      else if (
        horde.size == 2 && hordeRoles
          .count(r => r == Lance || r == Rook || r == Pawn) < 2 && armyNonQueens.size <= 1
      )
        true
      else fortress
    } else fortress
  }
}
