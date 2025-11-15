package lila.plan
package ui

import lila.ui.*
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.plan.{ PatronTier, PatronColor }

final class PlanStyle(helpers: Helpers):
  import helpers.{ *, given }

  def selector(using ctx: Context) = for
    me <- ctx.me
    plan <- ctx.me.map(_.plan).filter(_.active)
    patron <- me.patronAndColor
  yield div(cls := "patron-style-selector")(
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
          span(cls := s"patron-icon--shiny patron-icon--shiny--${color.id}")(i),
          span(cls := "pss__color__tier-name")(tier.name)
        )
    ),
    div(cls := "patron-style-selector__info")(
      p(
        "Thank you ",
        userLink(me),
        " for supporting Lichess ",
        if plan.lifetime then "as a Lifetime Patron"
        else frag("for ", strong(pluralize("month", plan.months))),
        ". You may choose one of the wing colors you unlocked."
      )
    )
  )
