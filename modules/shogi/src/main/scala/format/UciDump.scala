package shogi
package format

import cats.data.Validated

import shogi.variant.Variant

object UsiDump {

  // a2a4, b8c6
  def apply(replay: Replay): List[String] =
    replay.chronoMoves map move

  def apply(
      moves: Seq[String],
      initialFen: Option[String],
      variant: Variant
  ): Validated[String, List[String]] =
    if (moves.isEmpty) Validated.valid(Nil)
    else Replay(moves, initialFen, variant) andThen (_.valid) map apply

  def move(mod: MoveOrDrop): String =
    mod match {
      case Left(m)  => m.toUsi.usi
      case Right(d) => d.toUsi.usi
    }
}
