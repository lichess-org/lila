package views.html.simul

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.hub.LightTeam
import lila.simul.Simul
import lila.simul.SimulForm
import lila.gathering.ConditionForm

object form:

  def create(form: Form[SimulForm.Setup], teams: List[LightTeam])(using PageContext) =
    views.html.base.layout(
      title = trans.hostANewSimul.txt(),
      moreCss = cssTag("simul.form"),
      moreJs = jsModule("flatpickr")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(cls := "box__top")(trans.hostANewSimul()),
        postForm(cls := "form3", action := routes.Simul.create)(
          br,
          p(trans.whenCreateSimul()),
          br,
          br,
          formContent(form, teams, none),
          form3.actions(
            a(href := routes.Simul.home)(trans.cancel()),
            form3.submit(trans.hostANewSimul(), icon = licon.Trophy.some)
          )
        )
      )
    }

  def edit(form: Form[SimulForm.Setup], teams: List[LightTeam], simul: Simul)(using PageContext) =
    views.html.base.layout(
      title = s"Edit ${simul.fullName}",
      moreCss = cssTag("simul.form"),
      moreJs = jsModule("flatpickr")
    ) {
      main(cls := "box box-pad page-small simul-form")(
        h1(cls := "box__top")("Edit ", simul.fullName),
        postForm(cls := "form3", action := routes.Simul.update(simul.id))(
          formContent(form, teams, simul.some),
          form3.actions(
            a(href := routes.Simul.show(simul.id))(trans.cancel()),
            form3.submit(trans.save(), icon = licon.Trophy.some)
          )
        ),
        postForm(cls := "terminate", action := routes.Simul.abort(simul.id))(
          submitButton(dataIcon := licon.CautionCircle, cls := "text button button-red confirm")(
            trans.cancelSimul()
          )
        )
      )
    }

  private def formContent(form: Form[SimulForm.Setup], teams: List[LightTeam], simul: Option[Simul])(using
      ctx: PageContext
  ) =
    import lila.simul.SimulForm.*
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
      form3.fieldset("Games")(
        form3.group(form("variant"), trans.simulVariantsHint()) { f =>
          frag(
            div(cls := "variants")(
              views.html.setup.filter.renderCheckboxes(
                form,
                "variants",
                translatedVariantChoicesWithVariants,
                checks = form.value
                  .map(_.variants.map(_.toString))
                  .getOrElse(simul.so(_.variants.map(_.id.toString)))
                  .toSet
              )
            ),
            errMsg(f)
          )
        },
        form3.split(
          form3.group(
            form("position"),
            trans.startPosition(),
            klass = "position",
            half = true,
            help = trans
              .positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.boardEditor.txt()))
              .some
          )(form3.input(_)),
          form3.group(form("color"), trans.simulHostcolor(), half = true)(
            form3.select(_, colorChoices)
          )
        )
      ),
      form3.fieldset("Clock")(
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
          form3.group(
            form("clockExtraPerPlayer"),
            trans.simulHostExtraTimePerPlayer(),
            help = trans.simulAddExtraTimePerPlayer().some,
            half = true
          )(
            form3.select(_, clockExtraPerPlayerChoices)
          )
        )
      ),
      form3.fieldset("Entry conditions")(
        form3.split(
          teams.nonEmpty option
            form3.group(form("conditions.team.teamId"), trans.onlyMembersOfTeam(), half = true)(
              form3.select(_, List(("", trans.noRestriction.txt())) ::: teams.map(_.pair))
            )
        ),
        form3.split(
          form3.group(form("conditions.minRating.rating"), trans.minimumRating(), half = true)(
            form3.select(_, ConditionForm.minRatingChoices)
          ),
          form3.group(form("conditions.maxRating.rating"), trans.maximumWeeklyRating(), half = true)(
            form3.select(_, ConditionForm.maxRatingChoices)
          )
        )
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
