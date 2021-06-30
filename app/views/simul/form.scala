package views.html.simul

import controllers.routes
import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.LeaderTeam
import lila.simul.Simul
import lila.simul.SimulForm

object form {

  def create(form: Form[SimulForm.Setup], teams: List[LeaderTeam])(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form"),
      moreJs = jsModule("flatpickr")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(trans.hostANewSimul()),
        postForm(cls := "form3", action := routes.Simul.create)(
          br,
          p(trans.whenCreateSimul()),
          br,
          br,
          formContent(form, teams, none),
          form3.actions(
            a(href := routes.Simul.home)(trans.cancel()),
            form3.submit(trans.hostANewSimul(), icon = "".some)
          )
        )
      )
    }

  def edit(form: Form[SimulForm.Setup], teams: List[LeaderTeam], simul: Simul)(implicit
      ctx: Context
  ) =
    views.html.base.layout(
      title = s"Edit ${simul.fullName}",
      moreCss = cssTag("simul.form"),
      moreJs = jsModule("flatpickr")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(s"Edit ${simul.fullName}"),
        postForm(cls := "form3", action := routes.Simul.update(simul.id))(
          formContent(form, teams, simul.some),
          form3.actions(
            a(href := routes.Simul.show(simul.id))(trans.cancel()),
            form3.submit(trans.save(), icon = "".some)
          )
        ),
        postForm(cls := "terminate", action := routes.Simul.abort(simul.id))(
          submitButton(dataIcon := "", cls := "text button button-red confirm")(
            trans.cancelSimul()
          )
        )
      )
    }

  private def formContent(form: Form[SimulForm.Setup], teams: List[LeaderTeam], simul: Option[Simul])(implicit
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
      form3.group(form("variant"), trans.simulVariantsHint()) { f =>
        frag(
          div(cls := "variants")(
            views.html.setup.filter.renderCheckboxes(
              form,
              "variants",
              translatedVariantChoicesWithVariants,
              checks = form.value
                .map(_.variants.map(_.toString))
                .getOrElse(simul.??(_.variants.map(_.id.toString)))
                .toSet
            )
          ),
          errMsg(f)
        )
      },
      form3.split(
        form3.group(
          form("clockTime"),
          trans.clockInitialTime(),
          help = trans.simulClockHint().some,
          half = true
        )(form3.select(_, clockTimeChoices)),
        form3.group(form("clockIncrement"), trans.clockIncrement(), half = true)(
          form3.select(_, clockIncrementChoices)
        )
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
        form3.group(form("color"), trans.simulHostcolor(), half = true)(
          form3.select(_, colorChoices)
        )
      ),
      form3.split(
        teams.nonEmpty option
          form3.group(form("team"), trans.onlyMembersOfTeam(), half = true)(
            form3.select(_, List(("", trans.noRestriction.txt())) ::: teams.map(_.pair))
          ),
        form3.group(
          form("position"),
          trans.startPosition(),
          klass = "position",
          half = true,
          help =
            trans.positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.boardEditor.txt())).some
        )(form3.input(_))
      ),
      form3.group(
        form("estimatedStartAt"),
        trans.estimatedStart(),
        half = true
      )(form3.flatpickr(_)),
      form3.group(
        form("text"),
        trans.simulDescription(),
        help = trans.simulDescriptionHelp().some
      )(form3.textarea(_)(rows := 10)),
      ctx.me.exists(_.canBeFeatured) option form3.checkbox(
        form("featured"),
        trans.simulFeatured("lichess.org/simul"),
        help = trans.simulFeaturedHelp("lichess.org/simul").some
      )
    )
  }
}
