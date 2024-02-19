package views.html.game

import chess.format.Fen
import controllers.routes
import play.api.i18n.Lang

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.game.Pov
import lila.i18n.defaultLang

object mini:

  private val dataLive        = attr("data-live")
  private val dataState       = attr("data-state")
  private val dataTime        = attr("data-time")
  private val dataTimeControl = attr("data-tc")
  val cgWrap                  = span(cls := "cg-wrap")(cgWrapContent)

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

  def noCtx(pov: Pov, tv: Boolean = false, channelKey: Option[String] = None): Tag =
    val link = if tv then channelKey.fold(routes.Tv.index){routes.Tv.onChannel} else routes.Round.watcher(pov.gameId, pov.color.name)
    renderMini(pov, link.url.some)(using defaultLang, None)

  private def renderMini(
      pov: Pov,
      link: Option[String] = None,
      showRatings: Boolean = true
  )(using Lang, Option[Me]): Tag =
    import pov.game
    val tag                                    = if link.isDefined then a else span
    def showTimeControl(c: chess.Clock.Config) = s"${c.limitSeconds}+${c.increment}"
    tag(
      href            := link,
      cls             := s"mini-game mini-game-${game.id} mini-game--init ${game.variant.key} is2d",
      dataLive        := game.isBeingPlayed.option(game.id),
      dataTimeControl := game.clock.map(_.config).fold("correspondence")(showTimeControl(_)),
      renderState(pov)
    )(
      renderPlayer(!pov, withRating = showRatings),
      cgWrap,
      renderPlayer(pov, withRating = showRatings)
    )

  def renderState(pov: Pov)(using me: Option[Me]) =
    val fen =
      if me.flatMap(pov.game.player).exists(_.blindfold) && pov.game.playable
      then chess.format.BoardAndColorFen("8/8/8/8/8/8/8/8 w")
      else Fen.writeBoardAndColor(pov.game.situation)

    dataState := s"${fen},${pov.color.name},${~pov.game.lastMoveKeys}"

  private def renderPlayer(pov: Pov, withRating: Boolean)(using Lang) =
    span(cls := "mini-game__player")(
      span(cls := "mini-game__user")(
        playerUsername(pov.player.light, pov.player.userId.flatMap(lightUser), withRating = false),
        withRating option span(cls := "rating")(lila.game.Namer ratingString pov.player)
      ),
      if pov.game.finished then renderResult(pov)
      else pov.game.clock.map { renderClock(_, pov.color) }
    )

  private def renderResult(pov: Pov) =
    span(cls := "mini-game__result"):
      pov.game.winnerColor.fold("½"): c =>
        if c == pov.color then "1" else "0"

  private def renderClock(clock: chess.Clock, color: chess.Color) =
    val s = clock.remainingTime(color).roundSeconds
    span(
      cls      := s"mini-game__clock mini-game__clock--${color.name}",
      dataTime := s
    ):
      f"${s / 60}:${s % 60}%02d"
