package lila.round
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import chess.variant.{ Variant, Crazyhouse }

final class RoundUi(helpers: Helpers):
  import helpers.{ *, given }

  def RoundPage(variant: Variant, title: String)(using ctx: PageContext) =
    Page(title)
      .cssTag(if variant == Crazyhouse then "round.zh" else "round")
      .cssTag(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .cssTag(ctx.pref.hasVoice.option("voice"))
      .cssTag(ctx.blind.option("round.nvui"))
      .zoom
      .csp(_.withPeer.withWebAssembly)

  def povOpenGraph(pov: Pov) =
    OpenGraph(
      image = cdnUrl(routes.Export.gameThumbnail(pov.gameId, None, None).url).some,
      title = titleGame(pov.game),
      url = s"$netBaseUrl${routes.Round.watcher(pov.gameId, pov.color.name).url}",
      description = describePov(pov)
    )

  // #TODO RoundUi
  def describePov(pov: Pov) =
    import pov.*
    val p1    = playerText(game.whitePlayer, withRating = true)
    val p2    = playerText(game.blackPlayer, withRating = true)
    val plays = if game.finishedOrAborted then "played" else "is playing"
    val speedAndClock =
      if game.sourceIs(_.Import) then "imported"
      else
        game.clock.fold(chess.Speed.Correspondence.name): c =>
          s"${chess.Speed(c.config).name} (${c.config.show})"

    val mode = game.mode.name
    val variant =
      if game.variant == chess.variant.FromPosition
      then "position setup chess"
      else if game.variant.exotic
      then game.variant.name
      else "chess"
    import chess.Status.*
    val result = (game.winner, game.loser, game.status) match
      case (Some(w), _, Mate)                               => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime)                          => s"${playerText(l)} ran out of time"
      case (Some(w), _, UnknownFinish | VariantEnd)         => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish)         => "Game is a draw"
      case (_, _, Aborted)                                  => "Game has been aborted"
      case _ if game.finished                               => "Game ended"
      case _                                                => "Game is still ongoing"
    val moves = (game.ply.value - game.startedAtPly.value + 1) / 2
    s"$p1 $plays $p2 in a $mode $speedAndClock game of $variant. $result after ${pluralize("move", moves)}. Click to replay, analyse, and discuss the game!"
