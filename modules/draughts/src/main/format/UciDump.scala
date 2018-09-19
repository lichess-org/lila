package draughts
package format

import scalaz.Validation.FlatMap._
import scalaz.Validation.success

import draughts.variant.Variant

object UciDump {

  // a2a4, b8c6
  def apply(replay: Replay): List[String] =
    replay.chronoMoves map move(replay.setup.board.variant)

  def apply(moves: Seq[String], initialFen: Option[String], variant: Variant, finalSquare: Boolean = false): Valid[List[String]] =
    moves.isEmpty.fold(
      success(Nil),
      Replay(moves, initialFen, variant, finalSquare) flatMap (_.valid) map apply
    )

  def move(variant: Variant)(mod: Move): String = mod.toUci.uci

}
