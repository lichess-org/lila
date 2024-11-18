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
      .css("bits.recap"):
        main(cls := "recap")(
          av match
            case Availability.Available(recap) => showRecap(recap)
            case _                             => showEmpty(av, user)
        )

  private def showRecap(recap: Recap)(using Context) = frag(
    h1(cls := "recap__title")(userIdLink(recap.id.some), " vs ", recap.year),
    div(cls := "recap__content")(
      div(cls := "recap__box recap__games")(
        strong(cls := "recap--massive")(recap.nbGames.localize),
        div(
          p("That's how many games you played this year. Whew!"),
          p(
            strong(cls := "recap--big")(lila.core.i18n.translateDuration(recap.timePlaying)),
            " that you will never get back."
          )
        )
      ),
      div(cls := "recap__perfs")(
        recap.significantPerfs.map: p =>
          val percent = p.seconds * 100d / recap.timePlaying.toSeconds
          val width   = 10 * Math.sqrt(percent)
          div(cls := "recap__perf")(
            span(cls := "recap__perf__duration", style := s"width:${percentNumber(width)}%"),
            span(cls := "recap__perf__data")(
              iconTag(p.key.perfIcon),
              span(cls := "recap__perf__games")(strong(p.games.localize), span(p.key.perfTrans, " games")),
              span(cls := "recap__perf__time")(lila.core.i18n.translateDuration(p.duration))
            )
          )
      )
      // fragList:
      //   recap.openings
      //     .mapWithColor: (color, c) =>
      //       div("Favourite opening as ", color.name, ": ", c.value.name, " (", c.count, " games)")
      //     .toList
      // ,
      // div("Results", recap.results.toString),
      // div(
      //   "Best opponent",
      //   recap.opponent.map: c =>
      //     frag(userIdLink(c.value.some), pluralize("game", c.count))
      // ),
      // div(
      //   "Preferred first move",
      //   recap.firstMove.map: c =>
      //     frag(c.value, pluralize("game", c.count))
      // )
    )
  )

  private def percentNumber(v: Double) = f"${v}%1.2f"

  private def showEmpty(av: Availability, user: User)(using Context) =
    main(cls := "recap box box-pad")(
      h1(cls := "box__top")(title(user)),
      av.toString
    )
