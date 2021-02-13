package views.html.swiss

import play.api.data.Form

import controllers.routes
import lila.api.Context
import lila.app.templating.Environment._
import lila.tournament.TournamentForm
import lila.swiss.{ Swiss, SwissForm }
import lila.app.ui.ScalatagsTemplate._
import lila.hub.LightTeam.TeamID

object form {

  def create(form: Form[_], teamId: TeamID)(implicit ctx: Context) =
    views.html.base.layout(
      title = "New Swiss tournament",
      moreCss = cssTag("swiss.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
      val fields = new SwissFields(form)
      main(cls := "page-small")(
        div(cls := "swiss__form tour__form box box-pad")(
          h1("New Swiss tournament"),
          postForm(cls := "form3", action := routes.Swiss.create(teamId))(
            form3.split(fields.name, fields.nbRounds),
            form3.split(fields.rated, fields.variant),
            fields.clock,
            fields.description,
            form3.split(
              fields.roundInterval,
              fields.startsAt
            ),
            form3.split(
              fields.chatFor,
              fields.password
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Team.show(teamId))(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = "g".some)
            )
          )
        )
      )
    }

  def edit(swiss: Swiss, form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = swiss.name,
      moreCss = cssTag("swiss.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
      val fields = new SwissFields(form)
      main(cls := "page-small")(
        div(cls := "swiss__form box box-pad")(
          h1("Edit ", swiss.name),
          postForm(cls := "form3", action := routes.Swiss.update(swiss.id.value))(
            form3.split(fields.name, fields.nbRounds),
            form3.split(fields.rated, fields.variant),
            fields.clock,
            fields.description,
            form3.split(
              fields.roundInterval,
              swiss.isCreated option fields.startsAt
            ),
            form3.split(
              fields.chatFor,
              fields.password
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Swiss.show(swiss.id.value))(trans.cancel()),
              form3.submit(trans.save(), icon = "g".some)
            )
          ),
          postForm(cls := "terminate", action := routes.Swiss.terminate(swiss.id.value))(
            submitButton(dataIcon := "j", cls := "text button button-red confirm")(
              "Cancel the tournament"
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
        small(cls := "form-help")(
          trans.safeTournamentName(),
          br,
          trans.inappropriateNameWarning(),
          br,
          trans.emptyTournamentName()
        )
      )
    }
  def nbRounds =
    form3.group(
      form("nbRounds"),
      "Number of rounds",
      help = raw("An odd number of rounds allows optimal color balance.").some,
      half = true
    )(
      form3.input(_, typ = "number")
    )

  def rated =
    frag(
      form3.checkbox(
        form("rated"),
        trans.rated(),
        help = raw("Games are rated<br>and impact players ratings").some
      ),
      st.input(tpe := "hidden", st.name := form("rated").name, value := "false") // hack allow disabling rated
    )
  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(_, translatedVariantChoicesWithVariants(_.key).map(x => x._1 -> x._2))
    )
  def clock =
    form3.split(
      form3.group(form("clock.limit"), trans.clockInitialTime(), half = true)(
        form3.select(_, SwissForm.clockLimitChoices)
      ),
      form3.group(form("clock.increment"), trans.clockIncrement(), half = true)(
        form3.select(_, TournamentForm.clockIncrementChoices)
      )
    )
  def roundInterval =
    form3.group(form("roundInterval"), frag("Interval between rounds"), half = true)(
      form3.select(_, SwissForm.roundIntervalChoices)
    )
  def description =
    form3.group(
      form("description"),
      frag("Tournament description"),
      help = frag(
        "Anything special you want to tell the participants? Try to keep it short. Markdown links are available: [name](https://url)"
      ).some
    )(form3.textarea(_)(rows := 4))
  def startsAt =
    form3.group(
      form("startsAt"),
      frag("Tournament start date"),
      help = frag("In your own local timezone").some,
      half = true
    )(form3.flatpickr(_))

  def chatFor =
    form3.group(form("chatFor"), frag("Tournament chat"), half = true) { f =>
      form3.select(
        f,
        Seq(
          Swiss.ChatFor.NONE    -> "No chat",
          Swiss.ChatFor.LEADERS -> "Only team leaders",
          Swiss.ChatFor.MEMBERS -> "Only team members",
          Swiss.ChatFor.ALL     -> "All Lichess players"
        )
      )
    }

  def password =
    form3.group(
      form("password"),
      trans.password(),
      help = trans.makePrivateTournament().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))
}
