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
      .css("recap"):
        main(cls := "recap box box-pad")(
          h1(cls := "box__top")(title(user)),
          av match
            case Availability.Available(recap) => showRecap(recap)
            case _                             => showEmpty(av, user)
        )

  private def showRecap(recap: Recap)(using Context) = div(
    div(recap.nbGames.localize, " games"),
    div(tps.timeSpentPlaying(), lila.core.i18n.translateDuration(recap.timePlaying)),
    fragList:
      recap.openings
        .mapWithColor: (color, c) =>
          div("Favourite opening as ", color.name, ": ", c.value.name, " (", c.count, " games)")
        .toList
    ,
    div("Results", recap.results.toString),
    div(
      "Best opponent",
      recap.opponent.map: c =>
        frag(userIdLink(c.value.some), pluralize("game", c.count))
    ),
    div(
      "Preferred first move",
      recap.firstMove.map: c =>
        frag(c.value, pluralize("game", c.count))
    )
  )

  private def showEmpty(av: Availability, user: User)(using Context) =
    av.toString
