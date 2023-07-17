package views.html.game

import chess.format.Fen
import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.game.Pov
import lila.i18n.defaultLang

object mini:

  private val dataLive  = attr("data-live")
  private val dataState = attr("data-state")
  private val dataTime  = attr("data-time")
  val cgWrap            = span(cls := "cg-wrap")(cgWrapContent)

  def apply(
      pov: Pov,
      ownerLink: Boolean = false,
      tv: Boolean = false,
      withLink: Boolean = true
  )(using ctx: Context): Tag =
    renderMini(
      pov,
      withLink.option(gameLink(pov.game, pov.color, ownerLink, tv)),
      showRatings = ctx.pref.showRatings
    )

  def noCtx(pov: Pov, tv: Boolean = false): Tag =
    val link = if tv then routes.Tv.index else routes.Round.watcher(pov.gameId, pov.color.name)
    renderMini(pov, link.url.some)(using defaultLang)

  private def renderMini(
      pov: Pov,
      link: Option[String] = None,
      showRatings: Boolean = true
  )(using Lang): Tag =
    val game   = pov.game
    val isLive = game.isBeingPlayed
    val tag    = if link.isDefined then a else span
    tag(
      href     := link,
      cls      := s"mini-game mini-game-${game.id} mini-game--init ${game.variant.key} is2d",
      dataLive := isLive.option(game.id),
      renderState(pov)
    )(
      renderPlayer(!pov, withRating = showRatings),
      cgWrap,
      renderPlayer(pov, withRating = showRatings)
    )

  def renderState(pov: Pov) =
    dataState := s"${Fen writeBoardAndColor pov.game.situation},${pov.color.name},${~pov.game.lastMoveKeys}"

  private def renderPlayer(pov: Pov, withRating: Boolean)(using Lang) =
    span(cls := "mini-game__player")(
      span(cls := "mini-game__user")(
        playerUsername(pov.player.light, withRating = false),
        withRating option span(cls := "rating")(lila.game.Namer ratingString pov.player)
      ),
      if pov.game.finished then renderResult(pov)
      else pov.game.clock.map { renderClock(_, pov.color) }
    )

  private def renderResult(pov: Pov) =
    span(cls := "mini-game__result")(
      pov.game.winnerColor.fold("½") { c =>
        if c == pov.color then "1" else "0"
      }
    )

  private def renderClock(clock: chess.Clock, color: chess.Color) =
    val s = clock.remainingTime(color).roundSeconds
    span(
      cls      := s"mini-game__clock mini-game__clock--${color.name}",
      dataTime := s
    )(
      f"${s / 60}:${s % 60}%02d"
    )
