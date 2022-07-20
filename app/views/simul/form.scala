package views.html.simul

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import lila.simul.Simul
import lila.hub.LightTeam
import lila.simul.SimulForm

object form {

  def create(form: Form[SimulForm.Setup], teams: List[LightTeam])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form"),
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStartLocal
      )
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(trans.hostANewSimul()),
        postForm(cls := "form3", action := routes.Simul.create)(
          br,
          p(trans.whenCreateSimul()),
          br,
          br,
          formContent(form, teams),
          form3.actions(
            a(href := routes.Simul.home)(trans.cancel()),
            form3.submit(trans.hostANewSimul(), icon = "g".some)
          )
        )
      )
    }

  def edit(form: Form[SimulForm.Setup], teams: List[LightTeam], simul: Simul)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"Edit ${simul.fullName}",
      moreCss = cssTag("simul.form"),
      moreJs = frag(
        flatpickrTag,
        delayFlatpickrStartLocal
      )
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(s"Edit ${simul.fullName}"),
        postForm(cls := "form3", action := routes.Simul.update(simul.id))(
          formContent(form, teams),
          form3.actions(
            a(href := routes.Simul.show(simul.id))(trans.cancel()),
            form3.submit(trans.save(), icon = "g".some)
          )
        )
      )
    }

  private def formContent(form: Form[SimulForm.Setup], teams: List[LightTeam])(implicit
      ctx: Context
  ) = {
    import lila.simul.SimulForm._
    frag(
      globalError(form),
      form3.group(form("name"), trans.name()) { f =>
        div(
          form3.input(f),
          " Simul",
          br,
          small(cls := "form-help")(trans.inappropriateNameWarning())
        )
      },
      form3.group(form("variants"), trans.simulVariantsHint()) { f =>
        frag(
          div(cls := "variants")(
            views.html.setup.filter.renderCheckboxes(
              form,
              "variants",
              translatedVariantChoices,
              checks = form.value
                .fold(
                  form.data.filter(_._1 startsWith "variants").map(_._2)
                )(_.variants.map(_.toString))
                .toSet
            )
          )
        )
      },
      form3.split(
        form3.group(
          form("clockTime"),
          trans.clockInitialTime(),
          // help = trans.simulClockHint().some,
          half = true
        )(form3.select(_, clockTimeChoices)),
        form3.group(
          form("clockByoyomi"),
          trans.clockByoyomi(),
          half = true
        )(form3.select(_, clockByoyomiChoices))
      ),
      form3.split(
        form3.group(
          form("clockIncrement"),
          trans.clockIncrement(),
          half = true
        )(form3.select(_, clockIncrementChoices)),
        form3.group(
          form("periods"),
          trans.numberOfByoyomiPeriods(),
          half = true
        )(form3.select(_, periodsChoices))
      ),
      form3.split(
        form3.group(
          form("clockExtra"),
          trans.simulHostExtraTime(),
          help = trans.simulAddExtraTime().some,
          half = true
        )(
          form3.select(_, clockExtraChoices)
        ),
        form3.group(form("color"), trans.hostColorForEachGame(), half = true)(
          form3.select(_, colorChoices)
        )
      ),
      form3.split(
        (teams.size > 0) ?? {
          form3.group(form("team"), trans.onlyMembersOfTeam(), half = true)(
            form3.select(_, List(("", "No Restriction")) ::: teams.map(_.pair))
          )
        },
        form3.group(
          form("position"),
          trans.startPosition(),
          klass = "position",
          half = true,
          help = frag(
            views.html.tournament.form.positionInputHelp,
            br,
            "Works only with one variant selected."
          ).some
        )(form3.input(_))
      ),
      form3.group(
        form("estimatedStartAt"),
        frag("Estimated start time"),
        half = true
      )(form3.flatpickr(_)),
      form3.group(
        form("text"),
        trans.simulDescription(),
        help = trans.anythingTellParticipants().some
      )(form3.textarea(_)(rows := 10))
    )

  }
}
