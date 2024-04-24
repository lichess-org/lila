package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.app.templating.Environment.{ *, given }
import lila.ui.ScalatagsTemplate.{ *, given }
import lila.core.team.LightTeam
import lila.tournament.Tournament
import lila.tournament.ui.*

object form:
  given prefix: FormPrefix = FormPrefix.empty

  val ui = TournamentForm(formHelper, i18nHelper, translatedVariantChoicesWithVariants)

  def create(form: Form[?], leaderTeams: List[LightTeam])(using PageContext) =
    views.html.base.layout(
      title = trans.site.newTournament.txt(),
      moreCss = cssTag("tournament.form"),
      modules = jsModule("bits.tourForm")
    ):
      val fields = ui.tourFields(form, none)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(cls := "box__top")(
            if fields.isTeamBattle then trans.arena.newTeamBattle()
            else trans.site.createANewTournament()
          ),
          postForm(cls := "form3", action := routes.Tournament.webCreate)(
            div(cls := "form-group")(
              a(
                dataIcon := Icon.InfoCircle,
                cls      := "text",
                href     := routes.Cms.lonePage("event-tips")
              )(trans.site.ourEventTips())
            ),
            ui.setupCreate(form, leaderTeams),
            form3.actions(
              a(href := routes.Tournament.home)(trans.site.cancel()),
              form3.submit(trans.site.createANewTournament(), icon = Icon.Trophy.some)
            )
          )
        ),
        div(cls := "box box-pad tour__faq")(tournament.faq())
      )

  def edit(tour: Tournament, form: Form[?], myTeams: List[LightTeam])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      modules = jsModule("bits.tourForm")
    ):
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(cls := "box__top")("Edit ", tour.name()),
          postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
            ui.setupEdit(tour, form, myTeams),
            form3.actions(
              a(href := routes.Tournament.show(tour.id))(trans.site.cancel()),
              form3.submit(trans.site.save(), icon = Icon.Trophy.some)
            )
          ),
          hr,
          br,
          br,
          postForm(cls := "terminate", action := routes.Tournament.terminate(tour.id)):
            submitButton(dataIcon := Icon.CautionCircle, cls := "text button button-red confirm"):
              trans.site.cancelTournament()
        )
      )
