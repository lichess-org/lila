package lila.recap
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.recap.Recap.Availability

final class RecapUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.perfStat as tps

  private def title(user: User)(using ctx: Context) =
    if ctx.is(user) then "Your yearly recap"
    else s"${user.username} yearly recap"

  def home(av: Availability, user: User)(using Context) =
    Page(title(user))
      .csp(_.withInlineIconFont) // swiper's data: font
      .js(esmInit("bits.recap"))
      .css("bits.recap"):
        main(cls := "recap")(
          av match
            case Availability.Available(recap) => recapSwiper(recap)
            case _                             => showEmpty(av, user)
        )

  private def recapSwiper(recap: Recap)(using Context) =
    div(cls := "swiper")(
      div(cls := "swiper-wrapper")(
        div(cls := "swiper-slide")("Slide 1"),
        div(cls := "swiper-slide")("Slide 2"),
        div(cls := "swiper-slide")("Slide 3")
      ),
      div(cls := "swiper-pagination")
      // div(cls := "swiper-button-prev"),
      // div(cls := "swiper-button-next")
    )

  private def showRecap(recap: Recap)(using Context) =
    import recap.*
    frag(
      h1(cls := "recap__title")(userIdLink(recap.id.some), " vs ", recap.year),
      div(cls := "recap__content")(
        div(cls := "recap__box recap__games")(
          strong(cls := "recap--massive")(games.nb.nb.localize),
          div(
            p("That's how many games you played this year. Whew!"),
            p(
              strong(cls := "recap--big")(lila.core.i18n.translateDuration(games.timePlaying)),
              " that you will never get back."
            )
          )
        ),
        div(cls := "recap__perfs")(
          games.significantPerfs.map: p =>
            val percent = p.seconds * 100d / games.timePlaying.toSeconds
            val width   = 10 * Math.sqrt(percent)
            div(cls := "recap__perf")(
              span(cls := "recap__perf__duration", style := s"width:${percentNumber(width)}%"),
              span(cls := "recap__perf__data")(
                iconTag(p.key.perfIcon),
                span(cls := "recap__perf__games")(strong(p.games.localize), span(p.key.perfTrans, " games")),
                span(cls := "recap__perf__time")(lila.core.i18n.translateDuration(p.duration))
              )
            )
        ),
        games.openings
          .mapWithColor: (color, c) =>
            div("Favourite opening as ", color.name, ": ", c.value.name, " (", c.count, " games)")
          .toList,
        div(
          "Results",
          p(pluralizeLocalize("win", games.results.win)),
          p(pluralizeLocalize("draw", games.results.draw)),
          p(games.results.loss.localize, if games.results.loss == 1 then " loss" else " losses")
        ),
        div(
          "Best opponent",
          games.opponent.map: c =>
            p(userIdLink(c.value.some), pluralize("game", c.count))
        ),
        div(
          "Preferred first move",
          games.firstMove.map: c =>
            p(strong(c.value), " played in ", pluralizeLocalize("game", c.count))
        )
      )
    )

  private def percentNumber(v: Double) = f"${v}%1.2f"

  private def showEmpty(av: Availability, user: User)(using Context) =
    main(cls := "recap box box-pad")(
      h1(cls := "box__top")(title(user)),
      av.toString
    )
