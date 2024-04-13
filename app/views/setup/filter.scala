package views.html.setup

import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.web.ui.ScalatagsTemplate.{ *, given }
import lila.core.rating.RatingRange

object filter:

  def apply(form: Form[?])(using ctx: PageContext) =
    frag(
      st.form(novalidate)(
        table(
          tbody(
            tr(cls := "variant")(
              td(trans.site.variant()),
              td(
                renderCheckboxes(
                  form,
                  "variant",
                  translatedVariantChoicesWithVariants(_.key)
                )
              )
            ),
            tr(
              td(trans.site.timeControl()),
              td(renderCheckboxes(form, "speed", translatedSpeedChoices))
            ),
            tr(cls := "inline")(
              td(trans.site.increment()),
              td(
                renderCheckboxes(
                  form,
                  "increment",
                  translatedIncrementChoices
                )
              )
            ),
            ctx.isAuth.option(
              tr(cls := "inline")(
                td(trans.site.mode()),
                td(renderCheckboxes(form, "mode", translatedModeChoices))
              )
            ),
            ctx.isAuth.option(
              tr(
                td(trans.site.ratingRange()),
                td(
                  label(cls := "range")("? - ?"),
                  div(cls := "rating-range") {
                    val field = form("ratingRange")
                    frag(
                      form3.hidden(field),
                      input(
                        name := s"${field.name}_range_min",
                        tpe  := "range",
                        cls  := "range rating-range__min",
                        min  := RatingRange.min,
                        max  := RatingRange.max
                      ),
                      "/",
                      input(
                        name := s"${field.name}_range_max",
                        tpe  := "range",
                        cls  := "range rating-range__max",
                        min  := RatingRange.min,
                        max  := RatingRange.max
                      )
                    )
                  }
                )
              )
            )
          )
        ),
        div(cls := "actions")(
          button(
            tpe      := "reset",
            cls      := "button button-empty button-red text reset",
            dataIcon := Icon.NotAllowed
          )(
            trans.site.reset()
          ),
          submitButton(cls := "button button-green text apply", dataIcon := Icon.Checkmark)(
            trans.site.apply()
          )
        )
      )
    )

  def renderCheckboxes(
      form: Form[?],
      key: String,
      options: Seq[(Any, String, Option[String])],
      checks: Set[String] = Set.empty
  ): Frag =
    options.mapWithIndex { case ((value, text, hint), index) =>
      div(cls := "checkable")(
        renderCheckbox(form, key, index, value.toString, raw(text), hint, checks)
      )
    }

  private def renderCheckbox(
      form: Form[?],
      key: String,
      index: Int,
      value: String,
      content: Frag,
      hint: Option[String],
      checks: Set[String]
  ) =
    label(title := hint)(
      input(
        tpe      := "checkbox",
        cls      := "regular-checkbox",
        name     := s"${form(key).name}[$index]",
        st.value := value,
        checks(value).option(checked)
      ),
      content
    )
