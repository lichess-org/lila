package lila.round
package ui

import chess.variant.{ Crazyhouse, Variant }

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class RoundUi(helpers: Helpers, gameUi: lila.game.ui.GameUi):
  import helpers.{ *, given }

  def RoundPage(variant: Variant, title: String)(using ctx: Context) =
    Page(title)
      .css(if variant == Crazyhouse then "round.zh" else "round")
      .css(ctx.pref.hasKeyboardMove.option("keyboardMove"))
      .css(ctx.pref.hasVoice.option("voice"))
      .css(ctx.blind.option("round.nvui"))
      .i18nOpt(ctx.speechSynthesis, _.nvui)
      .i18nOpt(ctx.blind, _.keyboardMove)
      .flag(_.zoom)
      .csp(_.withPeer.withWebAssembly)

  def povOpenGraph(pov: Pov) =
    OpenGraph(
      image = cdnUrl(routes.Export.gameThumbnail(pov.gameId, None, None).url).some,
      title = titleGame(pov.game),
      url = routeUrl(routes.Round.watcher(pov.gameId, pov.color)),
      description = describePov(pov)
    )

  def others(playing: List[Pov], simul: Option[Frag])(using Context) =
    val switchId = "round-toggle-autoswitch"
    frag(
      h3(
        simul | frag(trans.site.currentGames()),
        span(
          cls := "move-on switcher",
          st.title := trans.site.automaticallyProceedToNextGameAfterMoving.txt()
        )(
          label(`for` := switchId)(trans.site.autoSwitch()),
          span(cls := "switch")(form3.cmnToggle(switchId, switchId, checked = false))
        )
      ),
      div(cls := "now-playing"):
        val (myTurn, otherTurn) = playing.partition(_.isMyTurn)
        (myTurn ++ otherTurn.take(8 - myTurn.size))
          .take(12)
          .map: pov =>
            a(href := routes.Round.player(pov.fullId), cls := pov.isMyTurn.option("my_turn"))(
              span(
                cls := s"mini-game mini-game--init ${pov.game.variant.key} is2d",
                gameUi.mini.renderState(pov)
              )(gameUi.mini.cgWrap),
              span(cls := "meta")(
                playerUsername(
                  pov.opponent.light,
                  pov.opponent.userId.flatMap(lightUserSync),
                  withRating = false,
                  withTitle = true
                ),
                span(cls := "indicator")(
                  if pov.isMyTurn then
                    pov.remainingSeconds
                      .fold[Frag](trans.site.yourTurn())(secondsFromNow(_, alwaysRelative = true))
                  else nbsp
                )
              )
            )
    )

  def describePov(pov: Pov) =
    import pov.*
    val p1 = playerText(game.whitePlayer, withRating = true)
    val p2 = playerText(game.blackPlayer, withRating = true)
    val plays = if game.finishedOrAborted then "played" else "is playing"
    val speedAndClock =
      if game.sourceIs(_.Import) then "imported"
      else
        game.clock.fold(chess.Speed.Correspondence.name): c =>
          s"${chess.Speed(c.config).name} (${c.config.show})"

    val rated = game.rated.name
    val variant =
      if game.variant == chess.variant.FromPosition
      then "position setup chess"
      else if game.variant.exotic
      then game.variant.name
      else "chess"
    import chess.Status.*
    val result = (game.winner, game.loser, game.status) match
      case (Some(w), _, Mate) => s"${playerText(w)} won by checkmate"
      case (_, Some(l), Resign | Timeout | Cheat | NoStart) => s"${playerText(l)} resigned"
      case (_, Some(l), Outoftime) => s"${playerText(l)} ran out of time"
      case (Some(w), _, UnknownFinish | VariantEnd) => s"${playerText(w)} won"
      case (_, _, Draw | Stalemate | UnknownFinish) => "Game is a draw"
      case (_, _, Aborted) => "Game has been aborted"
      case _ if game.finished => "Game ended"
      case _ => "Game is still ongoing"
    val moves = (game.ply.value - game.startedAtPly.value + 1) / 2
    s"$p1 $plays $p2 in a $rated $speedAndClock game of $variant. $result after ${pluralize("move", moves)}. Click to replay, analyse, and discuss the game!"

  def povChessground(pov: Pov)(using ctx: Context): Frag =
    chessground(
      board = pov.game.position,
      orient = pov.color,
      lastMove = pov.game.history.lastMove
        .map(_.origDest)
        .so: (orig, dest) =>
          List(orig, dest),
      blindfold = pov.player.blindfold,
      pref = ctx.pref
    )

  def roundAppPreload(pov: Pov)(using Context): Tag =
    div(cls := "round__app")(
      div(cls := "round__app__board main-board")(povChessground(pov)),
      div(cls := "col1-rmoves-preload")
    )
