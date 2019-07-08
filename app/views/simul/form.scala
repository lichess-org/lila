package views.html.simul

import play.api.data.Form
import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def apply(form: Form[lila.simul.SimulForm.Setup], teams: lila.hub.lightTeam.TeamIdsWithNames)(implicit ctx: Context) = {

    import lila.simul.SimulForm._

    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form")
    ) {
        main(cls := "box box-pad page-small simul-form")(
          h1(trans.hostANewSimul()),
          st.form(cls := "form3", action := routes.Simul.create(), method := "POST")(
            br, br,
            p(trans.whenCreateSimul()),
            br, br,
            globalError(form),
            form3.group(form("variant"), trans.simulVariantsHint()) { f =>
              div(cls := "variants")(
                views.html.setup.filter.renderCheckboxes(form, "variants", form.value.map(_.variants.map(_.toString)).getOrElse(Nil), translatedVariantChoicesWithVariants)
              )
            },
            form3.split(
              form3.group(form("clockTime"), raw("Clock initial time"), help = trans.simulClockHint().some, half = true)(form3.select(_, clockTimeChoices)),
              form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, clockIncrementChoices))
            ),
            form3.split(
              form3.group(form("clockExtra"), trans.simulHostExtraTime(), help = trans.simulAddExtraTime().some, half = true)(
                form3.select(_, clockExtraChoices)
              ),
              form3.group(form("color"), raw("Host color for each game"), half = true)(form3.select(_, colorChoices))
            ),
            (teams.size > 0) ?? {
              form3.group(form("team"), raw("Only members of team"), half = false)(form3.select(_, List(("", "No Restriction")) ::: teams))
            },
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
