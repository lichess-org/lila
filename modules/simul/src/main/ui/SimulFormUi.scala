package lila.simul
package ui

import play.api.data.{ Field, Form }

import lila.core.i18n.Translate
import lila.core.team.LightTeam
import lila.gathering.ui.GatheringFormUi
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SimulFormUi(helpers: Helpers)(
    setupCheckboxes: (Field, Seq[(Any, String, Option[String])], Set[String]) => Frag,
    translatedVariantChoicesWithVariantsById: Translate ?=> List[(String, String, Option[String])]
):
  import helpers.{ *, given }
  import SimulForm.EitherForm

  def create(form: Form[SimulForm.Setup], teams: List[LightTeam])(using Context) =
    Page(trans.site.hostANewSimul.txt())
      .css("simul.form")
      .js(Esm("bits.flatpickr")):
        main(cls := "box box-pad page-small simul-form")(
          h1(cls := "box__top")(trans.site.hostANewSimul()),
          postForm(cls := "form3", action := routes.Simul.create)(
            br,
            p(trans.site.whenCreateSimul()),
            br,
            br,
            formContent(Right(form), teams, none),
            form3.actions(
              a(href := routes.Simul.home)(trans.site.cancel()),
              form3.submit(trans.site.hostANewSimul(), icon = Icon.Trophy.some)
            )
          )
        )

  def edit(form: EitherForm, teams: List[LightTeam], simul: Simul)(using Context) =
    Page(s"Edit ${simul.fullName}")
      .css("simul.form")
      .js(Esm("bits.flatpickr")):
        main(cls := "box box-pad page-small simul-form")(
          h1(cls := "box__top")("Edit ", simul.fullName),
          postForm(cls := "form3", action := routes.Simul.update(simul.id))(
            formContent(form, teams, simul.some),
            form3.actions(
              a(href := routes.Simul.show(simul.id))(trans.site.cancel()),
              form3.submit(trans.site.save(), icon = Icon.Trophy.some)
            )
          ),
          form.isRight.option:
            postForm(cls := "terminate", action := routes.Simul.abort(simul.id))(
              submitButton(dataIcon := Icon.CautionCircle, cls := "text button button-red yes-no-confirm")(
                trans.site.cancelSimul()
              )
            )
        )

  private val gatheringFormUi = GatheringFormUi(helpers)

  private def formContent(form: EitherForm, teams: List[LightTeam], simul: Option[Simul])(using
      ctx: Context
  ) =
    import SimulForm.*
    val anyForm = form.merge
    frag(
      globalError(anyForm),
      form3.group(anyForm("name"), trans.site.name()) { f =>
        div(
          form3.input(f),
          " Simul",
          br,
          small(cls := "form-help")(trans.site.inappropriateNameWarning())
        )
      },
      form.toOption.map: form =>
        form3.fieldset("Games")(
          form3.group(form("variant"), trans.site.simulVariantsHint()) { f =>
            frag(
              div(cls := "variants")(
                setupCheckboxes(
                  form("variants"),
                  translatedVariantChoicesWithVariantsById,
                  form.value
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
              trans.site.startPosition(),
              klass = "position",
              half = true,
              help = trans.site
                .positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.site.boardEditor.txt()))
                .some
            )(form3.input(_)),
            form3.group(form("color"), trans.site.simulHostcolor(), half = true)(
              form3.select(_, colorChoices)
            )
          )
        ),
      form.toOption.map: form =>
        form3.fieldset("Clock")(
          form3.split(
            form3.group(
              form("clockTime"),
              trans.site.clockInitialTime(),
              help = trans.site.simulClockHint().some,
              half = true
            )(form3.select(_, clockTimeChoices)),
            form3.group(form("clockIncrement"), trans.site.clockIncrement(), half = true)(
              form3.select(_, clockIncrementChoices)
            )
          ),
          form3.split(
            form3.group(
              form("clockExtra"),
              trans.site.simulHostExtraTime(),
              help = trans.site.simulAddExtraTime().some,
              half = true
            )(
              form3.select(_, clockExtraChoices)
            ),
            form3.group(
              form("clockExtraPerPlayer"),
              trans.site.simulHostExtraTimePerPlayer(),
              help = trans.site.simulAddExtraTimePerPlayer().some,
              half = true
            )(
              form3.select(_, clockExtraPerPlayerChoices)
            )
          )
        ),
      form.toOption.map: form =>
        form3.fieldset("Entry conditions")(
          form3.split(
            teams.nonEmpty.option(
              form3.group(form("conditions.team.teamId"), trans.site.onlyMembersOfTeam(), half = true)(
                form3.select(_, List(("", trans.site.noRestriction.txt())) ::: teams.map(_.pair))
              )
            ),
            gatheringFormUi.accountAge(form("conditions.accountAge"))
          ),
          form3.split(
            gatheringFormUi.minRating(form("conditions.minRating.rating")),
            gatheringFormUi.maxRating(form("conditions.maxRating.rating"))
          )
        ),
      form.toOption.map: form =>
        form3.split(
          form3.group(
            form("estimatedStartAt"),
            trans.site.estimatedStart(),
            half = true
          )(form3.flatpickr(_))
        ),
      form3.group(
        anyForm("text"),
        trans.site.simulDescription(),
        help = trans.site.simulDescriptionHelp().some
      )(form3.textarea(_)(rows := 10)),
      form.toOption.map: form =>
        ctx.me
          .exists(canBeFeatured)
          .option:
            form3.checkboxGroup(
              form("featured"),
              trans.site.simulFeatured("lichess.org/simul"),
              help = trans.site.simulFeaturedHelp("lichess.org/simul").some
            )
    )
