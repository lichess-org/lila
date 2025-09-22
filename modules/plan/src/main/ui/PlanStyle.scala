package lila.plan
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.plan.{ PatronTier, PatronColor }

final class PlanStyle(helpers: Helpers):
  import helpers.{ *, given }

  def selector(patron: PatronTier.AndColor)(using Context) =
    div(cls := "patron-style-selector")(
      div(cls := "pss__colors")(
        PatronColor.values.map: color =>
          val tier = PatronTier.byColor(color)
          div(
            cls := List(
              s"pss__color pss__color--${color.id}" -> true,
              "pss__color--active" -> (patron.color == color),
              "pss__color--selectable" -> color.selectable(patron.tier)
            )
          )(
            span(cls := "user-link online")(
              iconTag(Icon.Wings)(cls := s"line patron ${color.cssClass}"),
              span(cls := "pss__color__tier-name")(tier.name)
            )
          )
      )
    )
