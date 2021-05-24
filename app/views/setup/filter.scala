package views.html.setup

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.RatingRange

object filter {

  import bits._

  def apply(form: Form[_])(implicit ctx: Context) =
    frag(
      cssTag("lobby.setup"),
      st.form(novalidate)(
        table(
          tbody(
            tr(cls := "variant")(
              td(trans.variant()),
              td(
                renderCheckboxes(
                  form,
                  "variant",
                  translatedVariantChoicesWithVariants(_.key)
                )
              )
            ),
            tr(
              td(trans.timeControl()),
              td(renderCheckboxes(form, "speed", translatedSpeedChoices))
            ),
            tr(cls := "inline")(
              td(trans.increment()),
              td(
                renderCheckboxes(
                  form,
                  "increment",
                  translatedIncrementChoices
                )
              )
            ),
            ctx.isAuth option tr(cls := "inline")(
              td(trans.mode()),
              td(renderCheckboxes(form, "mode", translatedModeChoices))
            ),
            ctx.isAuth option tr(
              td(trans.ratingRange()),
              td(
                label(cls := "range")("? - ?"),
                div(cls := "rating-range") {
                  val field = form("ratingRange")
                  frag(
                    form3.hidden(field),
                    input(
                      name := s"${field.name}_range_min",
                      tpe := "range",
                      cls := "range rating-range__min",
                      min := RatingRange.min,
                      max := RatingRange.max
                    ),
                    "/",
                    input(
                      name := s"${field.name}_range_max",
                      tpe := "range",
                      cls := "range rating-range__max",
                      min := RatingRange.min,
                      max := RatingRange.max
                    )
                  )
                }
              )
            )
          )
        ),
        div(cls := "actions")(
          button(tpe := "reset", cls := "button button-empty button-red text reset", dataIcon := "k")(trans.reset()),
          submitButton(cls := "button button-green text apply", dataIcon := "E")(trans.apply())
        )
      )
    )

  def renderCheckboxes(
      form: Form[_],
      key: String,
      options: Seq[(Any, String, Option[String])],
      checks: Set[String] = Set.empty
  ): Frag =
    options.zipWithIndex.map { case ((value, text, hint), index) =>
      div(cls := "checkable")(
        renderCheckbox(form, key, index, value.toString, raw(text), hint, checks)
      )
    }

  private def renderCheckbox(
      form: Form[_],
      key: String,
      index: Int,
      value: String,
      content: Frag,
      hint: Option[String],
      checks: Set[String]
  ) =
    label(title := hint)(
      input(
        tpe := "checkbox",
        cls := "regular-checkbox",
        name := s"${form(key).name}[$index]",
        st.value := value,
        checks(value) option checked
      ),
      content
    )
}
