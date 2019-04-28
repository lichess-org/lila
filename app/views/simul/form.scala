package views.html.simul

import play.api.data.Form
import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def apply(form: Form[lidraughts.simul.SimulSetup], config: lidraughts.simul.DataForm)(implicit ctx: Context) = {

    import config._
    import lidraughts.simul.DataForm._

    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form")
    ) {
        main(cls := "box box-pad page-small simul-form")(
          h1(trans.hostANewSimul()),
          st.form(cls := "form3", action := routes.Simul.create(), method := "POST")(
            br, br,
            p(cls := "help")(trans.whenCreateSimul()),
            br, br,
            globalError(form),
            form3.group(form("variant"), trans.simulVariantsHint()) { _ =>
              div(cls := "variants")(
                views.html.setup.filter.renderCheckboxes(form, "variants", form.value.map(_.variants.map(_.toString)).getOrElse(Nil), translatedVariantChoicesWithVariants)
              )
            },
            form3.split(
              form3.group(form("clockTime"), trans.clockInitialTime(), help = trans.simulClockHint().some, half = true)(form3.select(_, clockTimeChoices)),
              form3.group(form("clockIncrement"), trans.increment(), half = true)(form3.select(_, clockIncrementChoices))
            ),
            form3.split(
              form3.group(form("clockExtra"), trans.simulHostExtraTime(), help = trans.simulAddExtraTime().some, half = true)(form3.select(_, clockExtraChoices)),
              form3.group(form("color"), trans.simulHostColor(), half = true)(form3.select(_, translatedColorChoices))
            ),
            form3.group(form("targetPct"), trans.winningPercentage(), help = trans.simulTargetPercentageHint().some)(
              form3.input(_, typ = "number")(st.placeholder := trans.targetPercentage.txt(), st.min := 50, st.max := 100)
            ),
            form3.group(form("chat"), trans.chatAvailableFor(), help = trans.simulChatRestrictionsHint().some)(form3.select(_, translatedChatChoices)),
            form3.group(form("text"), raw("Simul description"), help = frag("Anything you want to tell the participants?").some)(form3.textarea(_)(rows := 10)),
            form3.actions(
              a(href := routes.Simul.home())(trans.cancel()),
              form3.submit(trans.hostANewSimul(), icon = "g".some)
            )
          )
        )
      }
  }
}
