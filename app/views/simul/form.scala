package views.html.simul

import play.api.data.Form
import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object form {

  def apply(form: Form[lidraughts.simul.SimulForm.Setup], teams: lidraughts.hub.lightTeam.TeamIdsWithNames, me: lidraughts.user.User)(implicit ctx: Context) = {

    import lidraughts.simul.SimulForm._

    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form")
    ) {
        main(cls := "box box-pad page-small simul-form")(
          h1(trans.hostANewSimul()),
          postForm(cls := "form3", action := routes.Simul.create())(
            br,
            p(cls := "help")(trans.whenCreateSimul()),
            br, br,
            globalError(form),
            canPickName(me) ?? form3.group(form("name"), trans.name()) { f =>
              div(
                form3.input(f),
                " Simul",
                br,
                small(cls := "form-help")(
                  trans.safeSimulName(), br,
                  trans.inappropriateNameWarning(), br,
                  trans.emptySimulName(), br
                )
              )
            },
            form3.group(form("variant"), trans.simulVariantsHint()) { f =>
              frag(
                div(cls := "variants")(
                  views.html.setup.filter.renderCheckboxes(
                    form,
                    "variants",
                    form.value.map(_.variants.map(_.toString)).getOrElse(Nil),
                    translatedVariantChoicesWithVariants
                  )
                ),
                errMsg(f)
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
            (teams.size > 0) ?? {
              form3.group(form("team"), trans.onlyMembersOfTeam(), half = false)(form3.select(_, List(("", trans.noRestriction.txt())) ::: teams))
            },
            form3.group(form("text"), trans.simulDescription(), help = trans.simulDescriptionHelp().some)(form3.textarea(_)(rows := 10)),
            form3.actions(
              a(href := routes.Simul.home())(trans.cancel()),
              form3.submit(trans.hostANewSimul(), icon = "g".some)
            )
          )
        )
      }
  }
}
