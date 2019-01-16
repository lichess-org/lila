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

    bits.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("form3.css")
    ) {
        div(id := "simul", cls := "form")(
          div(cls := "content_box small_box simul_box")(
            h1(trans.hostANewSimul.frag()),
            st.form(cls := "form3", action := routes.Simul.create(), method := "POST")(
              br, br,
              p(cls := "help")(trans.whenCreateSimul.frag()),
              br, br,
              globalError(form),
              form3.group(form("variant"), trans.simulVariantsHint.frag()) { f =>
                div(cls := "variants")(
                  views.html.setup.filter.renderCheckboxes(form, "variants", form.value.map(_.variants.map(_.toString)).getOrElse(Nil), translatedVariantChoicesWithVariants)
                )
              },
              form3.split(
                form3.group(form("clockTime"), trans.clockInitialTime.frag(), help = trans.simulClockHint.frag().some, half = true)(form3.select(_, clockTimeChoices)),
                form3.group(form("clockIncrement"), trans.increment.frag(), half = true)(form3.select(_, clockIncrementChoices))
              ),
              form3.split(
                form3.group(form("clockExtra"), trans.simulHostExtraTime.frag(), help = trans.simulAddExtraTime.frag().some, half = true)(form3.select(_, clockExtraChoices)),
                form3.group(form("color"), trans.simulHostColor.frag(), half = true)(form3.select(_, translatedColorChoices))
              ),
              form3.group(form("targetPct"), trans.winningPercentage.frag(), help = trans.simulTargetPercentageHint.frag().some)(
                form3.input(_, typ = "number")(st.placeholder := trans.targetPercentage.txt(), st.min := 50, st.max := 100)
              ),
              form3.group(form("chat"), trans.chatAvailableFor.frag(), help = trans.simulChatRestrictionsHint.frag().some)(form3.select(_, translatedChatChoices)),
              form3.actions(
                a(href := routes.Simul.home())(trans.cancel.frag()),
                form3.submit(trans.hostANewSimul.frag(), icon = "g".some)
              )
            )
          )
        )
      }
  }
}
