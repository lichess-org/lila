package lila.round

import lila.game.Step

case class AnaMove(
    orig: chess.Pos,
    dest: chess.Pos,
    variant: chess.variant.Variant,
    fen: String,
    path: String,
    promotion: Option[chess.PromotableRole]) {

  def step: Valid[Step] =
    chess.Game(variant.some, fen.some)(orig, dest, promotion) map {
      case (game, move) => Step(
        ply = game.turns,
        move = game.pgnMoves.lastOption.map { san =>
          Step.Move(move.orig, move.dest, san)
        },
        fen = chess.format.Forsyth >> game,
        check = game.situation.check,
        dests = game.situation.destinations)
    }
}
