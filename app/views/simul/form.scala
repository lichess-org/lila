package views.html.simul

import play.api.data.Form
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def apply(form: Form[lila.simul.SimulSetup], config: lila.simul.DataForm)(implicit ctx: Context) = {

    import config._

    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form")
    ) {
        main(cls := "box box-pad page-small simul-form")(
          h1(trans.hostANewSimul.frag()),
          st.form(cls := "form3", action := routes.Simul.create(), method := "POST")(
            br, br,
            p(trans.whenCreateSimul.frag()),
            br, br,
            globalError(form),
            form3.group(form("variant"), trans.simulVariantsHint.frag()) { f =>
              div(cls := "variants")(
                views.html.setup.filter.renderCheckboxes(form, "variants", form.value.map(_.variants.map(_.toString)).getOrElse(Nil), translatedVariantChoicesWithVariants)
              )
            },
            form3.split(
              form3.group(form("clockTime"), raw("Clock initial time"), help = trans.simulClockHint.frag().some, half = true)(form3.select(_, clockTimeChoices)),
              form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, clockIncrementChoices))
            ),
            form3.group(form("clockExtra"), trans.simulHostExtraTime.frag(), help = trans.simulAddExtraTime.frag().some)(
              form3.select(_, clockExtraChoices)
            ),
            form3.group(form("color"), raw("Host color for each game"))(form3.select(_, colorChoices)),
            form3.actions(
              a(href := routes.Simul.home())(trans.cancel.frag()),
              form3.submit(trans.hostANewSimul.frag(), icon = "g".some)
            )
          )
        )
      }
  }
}
