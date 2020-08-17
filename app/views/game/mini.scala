package views.html.game

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.game.Pov

import controllers.routes
import chess.format.Forsyth

object mini {

  private val dataLive  = attr("data-live")
  private val dataState = attr("data-state")
  private val dataTime  = attr("data-time")

  def apply(
      pov: Pov,
      ownerLink: Boolean = false,
      tv: Boolean = false,
      withTitle: Boolean = true,
      withLink: Boolean = true,
      withLive: Boolean = true
  )(implicit ctx: Context): Tag = {
    val game   = pov.game
    val isLive = withLive && game.isBeingPlayed
    val tag    = if (withLink) a else span
    tag(
      href := withLink.option(gameLink(game, pov.color, ownerLink, tv)),
      title := withTitle.option(gameTitle(game, pov.color)),
      cls := s"mini-game mini-game-${game.id} mini-game--init ${game.variant.key} is2d",
      dataLive := isLive.option(game.id),
      renderState(pov)
    )(
      renderPlayer(!pov),
      span(cls := "cg-wrap")(cgWrapContent),
      renderPlayer(pov)
    )
  }

  def noCtx(pov: Pov, tv: Boolean = false, blank: Boolean = false): Frag = {
    val game   = pov.game
    val isLive = game.isBeingPlayed
    a(
      href := (if (tv) routes.Tv.index() else routes.Round.watcher(pov.gameId, pov.color.name)),
      title := gameTitle(pov.game, pov.color),
      cls := s"mini-game mini-game-${game.id} mini-game--init is2d ${isLive ?? "mini-game--live"} ${game.variant.key}",
      dataLive := isLive.option(game.id),
      renderState(pov)
    )(
      renderPlayer(!pov),
      span(cls := "cg-wrap")(cgWrapContent),
      renderPlayer(pov)
    )
  }

  private def renderState(pov: Pov) =
    dataState := s"${Forsyth boardAndColor pov.game.situation},${pov.color.name},${~pov.game.lastMoveKeys}"

  private def renderPlayer(pov: Pov) =
    span(cls := "mini-game__player")(
      span(cls := "mini-game__user")(
        playerUsername(pov.player, withRating = false, withTitle = true),
        span(cls := "rating")(lila.game.Namer ratingString pov.player)
      ),
      pov.game.clock.map { renderClock(_, pov.color) }
    )

  private def renderClock(clock: chess.Clock, color: chess.Color) = {
    val s = clock.remainingTime(color).roundSeconds
    span(
      cls := s"mini-game__clock mini-game__clock--${color.name}",
      dataTime := s
    )(
      f"${s / 60}:${s % 60}%02d"
    )
  }
}
