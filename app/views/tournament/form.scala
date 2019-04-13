package views.html
package tournament

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.tournament.DataForm

import controllers.routes

object form {
  def apply(form: Form[_], config: DataForm, me: User, teams: lila.hub.tournamentTeam.TeamIdsWithNames)(implicit ctx: Context) = views.html.base.layout(
    title = trans.newTournament.txt(),
    moreCss = responsiveCssTag("tournament.form"),
    moreJs = frag(
      flatpickrTag,
      jsTag("tournamentForm.js")
    )
  )(main(cls := "page-small")(
      div(cls := "tour__form box box-pad")(
        h1(trans.createANewTournament()),
        st.form(cls := "form3", action := routes.Tournament.create, method := "POST")(
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
            st.input(`type` := "hidden", name := form("rated").name, value := "false"), // hack allow disabling rated
            form3.group(form("variant"), trans.variant.frag(), half = true)(form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2)))
          ),
          form3.group(form("position"), trans.startPosition.frag(), klass = "position")(tournament.startingPosition(_)),
          form3.split(
            form3.group(form("clockTime"), raw("Clock initial time"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
            form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
          ),
          form3.split(
            form3.group(form("minutes"), trans.duration.frag(), half = true)(form3.select(_, DataForm.minuteChoices)),
            form3.group(form("waitMinutes"), trans.timeBeforeTournamentStarts.frag(), half = true)(form3.select(_, DataForm.waitMinuteChoices))
          ),
          form3.globalError(form),
          fieldset(cls := "conditions")(
            legend(trans.advancedSettings()),
            errMsg(form("conditions")),
            p(
              strong(dataIcon := "!", cls := "text")(trans.recommendNotTouching()),
              " ",
              trans.fewerPlayers(),
              " ",
              a(cls := "show blue")(trans.showAdvancedSettings())
            ),
            div(cls := "form")(
              form3.group(form("password"), trans.password.frag(), help = raw("Make the tournament private, and restrict access with a password").some)(form3.input(_)),
              tournament.conditionForm(form, auto = true, teams = teams),
              input(`type` := "hidden", name := form("berserkable").name, value := "false"), // hack allow disabling berserk
              form3.group(form("startDate"), raw("Custom start date"), help = raw("""This overrides the "Time before tournament starts" setting""").some)(form3.flatpickr(_))
            )
          ),
          form3.actions(
            a(href := routes.Tournament.home())(trans.cancel()),
            form3.submit(trans.createANewTournament.frag(), icon = "g".some)
          )
        )
      ),
      div(cls := "box box-pad tour__faq")(tournament.faq())
    ))
}
