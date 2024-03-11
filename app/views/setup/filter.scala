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
      st.form(novalidate)(
        table(
          tbody(
            tr(cls := "variant")(
              td(trans.variant()),
              td(
                renderCheckboxes(
                  form,
                  "variant",
                  translatedVariantChoices(_.key)
                )
              )
            ),
            tr(cls := "f-real_time")(
              td(trans.timeControl()),
              td(renderCheckboxes(form, "speed", translatedSpeedChoices))
            ),
            tr(cls := "inline f-real_time")(
              td(trans.byoyomi()),
              td(
                renderCheckboxes(
                  form,
                  "byoyomi",
                  translatedBooleanFilterChoices
                )
              )
            ),
            tr(cls := "inline f-real_time")(
              td(trans.increment()),
              td(
                renderCheckboxes(
                  form,
                  "increment",
                  translatedBooleanFilterChoices
                )
              )
            ),
            tr(cls := "f-seeks days")(
              td(trans.daysPerTurn()),
              td(renderCheckboxes(form, "days", translatedCorresDaysChoices))
            ),
            ctx.isAuth option tr(cls := "inline")(
              td(trans.mode()),
              td(renderCheckboxes(form, "mode", translatedModeChoices))
            ),
            ctx.isAuth option tr(cls := "inline f-real_time")(
              td(trans.anonymous()),
              td(
                renderCheckboxes(
                  form,
                  "anonymous",
                  translatedBooleanYesFilterChoice
                )
              )
            ),
            ctx.isAuth option tr(
              td(trans.ratingRange()),
              td(
                label(cls := "range")("? - ?"),
                div(cls := "rating-range")(
                  renderInput(form("ratingRange"))(
                    dataMin := RatingRange.min,
                    dataMax := RatingRange.max
                  )
                )
              )
            )
          )
        ),
        ctx.isAnon option frag(
          renderInput(form("mode")),
          renderInput(form("ratingRange"))
        ),
        div(cls := "actions")(
          submitButton(cls := "button button-empty button-red text reset", dataIcon := "k")(trans.reset()),
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
        tpe      := "checkbox",
        cls      := "regular-checkbox",
        name     := s"${form(key).name}[$index]",
        st.value := value,
        checks(value) option checked
      )(content)
    )
}
