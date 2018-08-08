package lidraughts.game

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration._

import draughts.Division
import draughts.variant.Variant
import draughts.format.FEN

final class Divider {

  private val cache: Cache[Game.ID, Division] = Scaffeine()
    .expireAfterAccess(5 minutes)
    .build[Game.ID, Division]

  def apply(game: Game, initialFen: Option[FEN]): Division =
    apply(game.id, game.pdnMoves, game.variant, initialFen)

  def apply(id: Game.ID, pdnmoves: => PdnMoves, variant: Variant, initialFen: Option[FEN]) =
    if (!Variant.divisionSensibleVariants(variant)) Division.empty
    else cache.get(id, _ => draughts.Replay.boards(
      moveStrs = pdnmoves,
      initialFen = initialFen,
      variant = variant
    ).toOption.fold(Division.empty)(draughts.Divider.apply))
}
