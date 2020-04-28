package views.html
package tournament

import play.api.data.{ Field, Form }

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.user.User
import lidraughts.tournament.{ Tournament, Condition, DataForm }

import controllers.routes

object editForm {

  def apply(tour: Tournament, form: Form[_], config: DataForm, me: User, teams: lidraughts.hub.tournamentTeam.TeamIdsWithNames)(implicit ctx: Context) = views.html.base.layout(
    title = tour.fullName,
    moreCss = cssTag("tournament.form"),
    moreJs = frag(
      flatpickrTag,
      jsTag("tournamentForm.js")
    )
  )(main(cls := "page-small")(
      div(cls := "tour__form box box-pad")(
        h1("Edit ", tour.fullName,
          st.form(cls := "form3", action := routes.Tournament.update(tour.id), method := "POST")(
            DataForm.canPickName(me) ?? {
              form3.group(form("name"), trans.name.frag()) { f =>
                div(
                  form3.input(f), " Arena", br,
                  small(cls := "form-help")(
                    trans.safeTournamentName(), br,
                    trans.inappropriateNameWarning(), br,
                    trans.emptyTournamentName(), br
                  )
                )
              }
            },
            form3.split(
              form3.checkbox(form("rated"), trans.rated.frag(), help = raw("Games are rated<br>and impact players ratings").some),
              st.input(tpe := "hidden", name := form("rated").name, value := "false"), // hack allow disabling rated
              form3.group(form("variant"), trans.variant.frag(), half = true)(form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2)))
            ),
            form3.group(form("position"), trans.startPosition.frag(), klass = "position")(views.html.tournament.form.startingPosition(_)),
            form3.split(
              form3.group(form("clockTime"), raw("Clock initial time"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
              form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
            ),
            form3.split(
              if (DataForm.minutes contains tour.minutes) form3.group(form("minutes"), trans.duration.frag(), half = true)(form3.select(_, DataForm.minuteChoices))
              else form3.group(form("minutes"), trans.duration(), half = true)(form3.input(_)(tpe := "number"))
            ),
            form3.group(form("description"), trans.tournamentDescription.frag(), help = trans.tournamentDescriptionHelp.frag().some)(form3.textarea(_)(rows := 2)),
            form3.globalError(form),
            fieldset(cls := "conditions")(
              legend(trans.advancedSettings()),
              errMsg(form("conditions")),
              p(
                strong(dataIcon := "!", cls := "text")(trans.recommendNotTouching()),
                " ",
                trans.fewerPlayers(),
                " ",
                a(cls := "show")(trans.showAdvancedSettings())
              ),
              div(cls := "form")(
                form3.group(form("password"), trans.password.frag(), help = raw("Make the tournament private, and restrict access with a password").some)(form3.input(_)),
                views.html.tournament.form.condition(form, auto = true, teams = teams),
                input(tpe := "hidden", name := form("berserkable").name, value := "false"), // hack allow disabling berserk
                form3.group(form("startDate"), raw("Custom start date"), help = raw("""This overrides the "Time before tournament starts" setting""").some)(form3.flatpickr(_))
              )
            ),
            form3.actions(
              a(href := routes.Tournament.show(tour.id))(trans.cancel()),
              form3.submit(trans.save.frag(), icon = "g".some)
            )
          ),
          st.form(cls := "terminate", action := routes.Tournament.terminate(tour.id), method := "POST")(
            form3.submit(trans.cancelTheTournament.frag(), icon = "j".some, klass = "text button button-red confirm")
          ))
      )
    ))
}