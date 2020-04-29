package views.html.swiss

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.tournament.{ DataForm => TourForm }
import lila.swiss.SwissForm
import lila.app.ui.ScalatagsTemplate._
import lila.hub.LightTeam.TeamID

object form {

  def create(form: Form[_], teamId: TeamID)(implicit ctx: Context) =
    views.html.base.layout(
      title = "New Swiss tournament",
      moreCss = cssTag("clas"),
      moreJs = jsAt("compiled/clas.js")
    ) {
      val fields = new SwissFields(form)
      main(cls := "page-small")(
        div(cls := "swiss__form box box-pad")(
          h1("New Swiss tournament"),
          postForm(cls := "form3", action := routes.Swiss.create(teamId))(
            fields.name,
            form3.split(fields.rated, fields.variant),
            fields.clock,
            fields.description,
            form3.globalError(form),
            fields.startsAt,
            form3.actions(
              a(href := routes.Team.show(teamId))(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = "g".some)
            )
          )
        )
      )
    }
}

final private class SwissFields(form: Form[_])(implicit ctx: Context) {

  def name =
    form3.group(form("name"), trans.name()) { f =>
      div(
        form3.input(f),
        br,
        small(cls := "form-help")(
          trans.safeTournamentName(),
          br,
          trans.inappropriateNameWarning(),
          br,
          trans.emptyTournamentName()
        )
      )
    }

  def rated = form3.checkbox(
    form("rated"),
    trans.rated(),
    help = raw("Games are rated<br>and impact players ratings").some
  )
  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2))
    )
  def clock =
    form3.split(
      form3.group(form("clock.limit"), trans.clockInitialTime(), half = true)(
        form3.select(_, TourForm.clockTimeChoices)
      ),
      form3.group(form("clock.increment"), trans.clockIncrement(), half = true)(
        form3.select(_, TourForm.clockIncrementChoices)
      )
    )
  def description =
    form3.group(
      form("description"),
      frag("Tournament description"),
      help = frag("Anything special you want to tell the participants? Try to keep it short.").some
    )(form3.textarea(_)(rows := 2))
  def startsAt =
    form3.group(
      form("startsAt"),
      frag("Tournament start date")
    )(form3.flatpickr(_))
}
