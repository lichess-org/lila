package views.html.setup

import play.api.data.{ Form, Field }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.rating.RatingRange

import controllers.routes

object filter {

  import bits._

  def apply(form: Form[_], filter: lila.setup.FilterConfig)(implicit ctx: Context) = frag(
    cssTag("lobby.setup"),
    st.form(action := routes.Setup.filter(), novalidate)(
      table(
        tbody(
          tr(cls := "variant")(
            td(trans.variant()),
            td(renderCheckboxes(form, "variant", filter.variant.map(_.id.toString), translatedVariantChoicesWithVariants))
          ),
          tr(
            td(trans.timeControl()),
            td(renderCheckboxes(form, "speed", filter.speed.map(_.id.toString), translatedSpeedChoices))
          ),
          ctx.isAuth option tr(
            td(trans.mode()),
            td(renderCheckboxes(form, "mode", filter.mode.map(_.id.toString), translatedModeChoices))
          ),
          ctx.me.map { me =>
            tr(
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
          }
        )
      ),
      ctx.isAnon option frag(
        renderInput(form("mode")),
        renderInput(form("ratingRange"))
      ),
      div(cls := "actions")(
        button(tpe := "submit", cls := "button button-empty button-red text reset", dataIcon := "k")(trans.reset()),
        button(tpe := "submit", cls := "button button-green text apply", dataIcon := "E")(trans.apply())
      )
    )
  )

  def renderCheckboxes(
    form: Form[_],
    key: String,
    checks: List[String],
    options: Seq[(Any, String, Option[String])]
  )(implicit ctx: Context): Frag =
    options.zipWithIndex.map {
      case ((value, text, hint), index) => div(cls := "checkable")(
        renderCheckbox(form, key, index, value.toString, checks, raw(text), hint)
      )
    }

  private def renderCheckbox(
    form: Form[_],
    key: String,
    index: Int,
    value: String,
    checks: List[String],
    content: Frag,
    hint: Option[String]
  ) = label(title := hint)(
    input(
      tpe := "checkbox",
      name := s"${form(key).name}[$index]",
      st.value := value,
      checks.has(value) option checked
    )(content)
  )
}
