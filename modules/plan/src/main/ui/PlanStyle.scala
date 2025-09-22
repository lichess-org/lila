package lila.plan
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.plan.{ PatronTier, PatronColor }
import lila.core.user.Plan

final class PlanStyle(helpers: Helpers):
  import helpers.{ *, given }

  def selector(plan: Plan, patron: PatronTier.AndColor)(using me: Me)(using Context) =
    div(cls := "patron-style-selector")(
      postForm(cls := "pss__colors", action := routes.Plan.style)(
        PatronColor.values.map: color =>
          val tier = PatronTier.byColor(color)
          button(
            name := "color",
            value := color.id,
            color.selectable(patron.tier).not.option(disabled),
            cls := List(
              s"pss__color pss__color--${color.id}" -> true,
              "pss__color--active" -> (patron.color == color),
              "pss__color--selectable" -> color.selectable(patron.tier)
            )
          )(
            span(cls := "user-link online")(
              iconTag(Icon.Wings)(cls := s"line patron ${color.cssClass}")
            ),
            span(cls := "pss__color__tier-name")(tier.name)
          )
      ),
      div(cls := "patron-style-selector__info")(
        p(
          "Thank you ",
          userLink(me),
          " for supporting Lichess for ",
          strong(pluralize("month", plan.months)),
          ". ",
          "You may choose one of the wing colors you unlocked."
        )
      )
    )
