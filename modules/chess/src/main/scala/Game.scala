package chess

import format.{ pgn, Uci }

case class Game(
    situation: Situation,
    pgnMoves: Vector[String] = Vector(),
    clock: Option[Clock] = None,
    turns: Int = 0, // plies
    startedAtTurn: Int = 0
) {
  def apply(
      orig: Pos,
      dest: Pos,
      promotion: Boolean = false,
      metrics: MoveMetrics = MoveMetrics()
  ): Valid[(Game, Move)] = {
    situation.move(orig, dest, promotion).map(_.normalizeCastle withMetrics metrics) map { move =>
      apply(move) -> move
    }
  }

  def apply(move: Move): Game = {
    val newSituation = move situationAfter

    copy(
      situation = newSituation,
      turns = turns + 1,
      pgnMoves = pgnMoves :+ pgn.Dumper(situation, move, newSituation),
      clock = applyClock(move.metrics, newSituation.status.isEmpty)
    )
  }

  def drop(
      role: Role,
      pos: Pos,
      metrics: MoveMetrics = MoveMetrics()
  ): Valid[(Game, Drop)] =
    situation.drop(role, pos).map(_ withMetrics metrics) map { drop =>
      applyDrop(drop) -> drop
    }

  def applyDrop(drop: Drop): Game = {
    val newSituation = drop situationAfter

    copy(
      situation = newSituation,
      turns = turns + 1,
      pgnMoves = pgnMoves :+ pgn.Dumper(drop, newSituation),
      clock = applyClock(drop.metrics, newSituation.status.isEmpty)
    )
  }

  private def applyClock(metrics: MoveMetrics, gameActive: Boolean) =
    clock.map { c =>
      {
        val newC = c.step(metrics, gameActive)
        if (turns - startedAtTurn == 1) newC.start else newC
      }
    }

  def apply(uci: Uci.Move): Valid[(Game, Move)] = apply(uci.orig, uci.dest, uci.promotion)
  def apply(uci: Uci.Drop): Valid[(Game, Drop)] = drop(uci.role, uci.pos)
  def apply(uci: Uci): Valid[(Game, MoveOrDrop)] = {
    uci match {
      case u: Uci.Move => apply(u) map { case (g, m) => g -> Left(m) }
      case u: Uci.Drop => apply(u) map { case (g, d) => g -> Right(d) }
    }
  }

  def player = situation.color

  def board = situation.board

  def isStandardInit = board.pieces == chess.variant.Standard.pieces

  def halfMoveClock: Int = board.history.halfMoveClock

  /**
    * Fullmove number: The number of the full move.
    * It starts at 1, and is incremented after Black's move.
    */
  def fullMoveNumber: Int = 1 + turns / 2

  def moveString = s"${fullMoveNumber}${player.fold(".", "...")}"

  def withBoard(b: Board) = copy(situation = situation.copy(board = b))

  def updateBoard(f: Board => Board) = withBoard(f(board))

  def withPlayer(c: Color) = copy(situation = situation.copy(color = c))

  def withTurns(t: Int) = copy(turns = t)
}

object Game {
  def apply(variant: chess.variant.Variant): Game =
    new Game(
      Situation(Board init variant, White)
    )

  def apply(board: Board): Game = apply(board, White)

  def apply(board: Board, color: Color): Game = new Game(Situation(board, color))

  def apply(variantOption: Option[chess.variant.Variant], fen: Option[String]): Game = {
    val variant = variantOption | chess.variant.Standard
    val g       = apply(variant)
    fen
      .flatMap {
        format.Forsyth.<<<@(variant, _)
      }
      .fold(g) { parsed =>
        g.copy(
          situation = Situation(
            board = parsed.situation.board withVariant g.board.variant withCrazyData {
              parsed.situation.board.crazyData orElse g.board.crazyData
            },
            color = parsed.situation.color
          ),
          turns = parsed.turns
        )
      }
  }
}
