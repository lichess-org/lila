package draughts

import format.{ pdn, Uci }

case class DraughtsGame(
    situation: Situation,
    pdnMoves: Vector[String] = Vector(),
    clock: Option[Clock] = None,
    /**turns means plies here*/
    turns: Int = 0,
    startedAtTurn: Int = 0
) {
  def apply(
    orig: Pos,
    dest: Pos,
    promotion: Option[PromotableRole] = None,
    metrics: MoveMetrics = MoveMetrics(),
    finalSquare: Boolean = false
  ): Valid[(DraughtsGame, Move)] =
    situation.move(orig, dest, promotion, finalSquare).map { move =>
      apply(move) -> move
    }

  def apply(move: Move): DraughtsGame = {

    val newSituation = move.situationAfter

    if (newSituation.ghosts != 0) {
      copy(
        situation = newSituation,
        turns = turns,
        pdnMoves = pdnMoves :+ pdn.Dumper(situation, move, newSituation),
        clock = clock
      )
    } else {
      copy(
        situation = newSituation,
        turns = turns + 1,
        pdnMoves = pdnMoves :+ pdn.Dumper(situation, move, newSituation),
        clock = applyClock(move.metrics, newSituation.status.isEmpty)
      )
    }

  }

  private def applyClock(metrics: MoveMetrics, gameActive: Boolean) = clock.map { c =>
    {
      val newC = c.step(metrics, gameActive)
      if (turns - startedAtTurn == 1) newC.start else newC
    }
  }

  def apply(uci: Uci.Move): Valid[(DraughtsGame, Move)] = apply(uci.orig, uci.dest, uci.promotion)

  def displayTurns = if (situation.ghosts == 0) turns else turns + 1

  def player = situation.color

  def board = situation.board

  def isStandardInit = board.pieces == draughts.variant.Standard.pieces

  def halfMoveClock: Int = board.history.halfMoveClock

  /**
   * Fullmove number: The number of the full move.
   * It starts at 1, and is incremented after Black's move.
   */
  def fullMoveNumber: Int = 1 + turns / 2

  def withBoard(b: Board) = copy(situation = situation.copy(board = b))

  def updateBoard(f: Board => Board) = withBoard(f(board))

  def withPlayer(c: Color) = copy(situation = situation.copy(color = c))

  def withTurns(t: Int) = copy(turns = t)
}

object DraughtsGame {
  def apply(variant: draughts.variant.Variant): DraughtsGame = new DraughtsGame(
    Situation(Board init variant, White)
  )

  def apply(board: Board): DraughtsGame = apply(board, White)

  def apply(board: Board, color: Color): DraughtsGame = new DraughtsGame(Situation(board, color))

  def apply(variantOption: Option[draughts.variant.Variant], fen: Option[String]): DraughtsGame = {
    val variant = variantOption | draughts.variant.Standard
    val g = apply(variant)
    fen.flatMap {
      format.Forsyth.<<<@(variant, _)
    }.fold(g) { parsed =>
      g.copy(
        situation = Situation(
          board = parsed.situation.board withVariant g.board.variant,
          color = parsed.situation.color
        ),
        turns = parsed.turns
      )
    }
  }
}
