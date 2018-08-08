package lidraughts.puzzle

import draughts.format.{ Forsyth, Uci, UciCharPair }
import draughts.opening.FullOpeningDB
import lidraughts.game.Game
import lidraughts.tree

object TreeBuilder {

  def apply(game: Game, plies: Int): tree.Root = {
    draughts.Replay.gameMoveWhileValid(game.pdnMoves take plies, Forsyth.initial, game.variant) match {
      case (init, games, error) =>
        error foreach logChessError(game.id)
        val fen = Forsyth >> init
        val root = tree.Root(
          ply = init.turns,
          fen = fen,
          opening = FullOpeningDB findByFen fen
        )
        def makeBranch(index: Int, g: draughts.DraughtsGame, m: Uci.WithSan) = {
          val fen = Forsyth >> g
          tree.Branch(
            id = UciCharPair(m.uci),
            ply = g.turns,
            move = m,
            fen = fen,
            opening = FullOpeningDB findByFen fen
          )
        }
        games.zipWithIndex.reverse match {
          case Nil => root
          case ((g, m), i) :: rest => root prependChild rest.foldLeft(makeBranch(i + 1, g, m)) {
            case (node, ((g, m), i)) => makeBranch(i + 1, g, m) prependChild node
          }
        }
    }
  }

  private val logChessError = (id: String) => (err: String) =>
    logger.warn(s"TreeBuilder https://lidraughts.org/$id ${err.lines.toList.headOption}")
}
