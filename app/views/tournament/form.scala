package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.user.User
import lila.tournament.{ Condition, DataForm }

import controllers.routes

object form {

  def apply(form: Form[_], config: DataForm, me: User, teams: lila.hub.lightTeam.TeamIdsWithNames)(implicit ctx: Context) = views.html.base.layout(
    title = trans.newTournament.txt(),
    moreCss = cssTag("tournament.form"),
    moreJs = frag(
      flatpickrTag,
      jsTag("tournamentForm.js")
    )
  )(main(cls := "page-small")(
      div(cls := "tour__form box box-pad")(
        h1(trans.createANewTournament()),
        st.form(cls := "form3", action := routes.Tournament.create, method := "POST")(
          DataForm.canPickName(me) ?? {
            form3.group(form("name"), trans.name()) { f =>
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
            form3.checkbox(form("rated"), trans.rated(), help = raw("Games are rated<br>and impact players ratings").some),
            st.input(tpe := "hidden", name := form("rated").name, value := "false"), // hack allow disabling rated
            form3.group(form("variant"), trans.variant(), half = true)(form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2)))
          ),
          form3.group(form("position"), trans.startPosition(), klass = "position")(startingPosition(_)),
          form3.split(
            form3.group(form("clockTime"), raw("Clock initial time"), half = true)(form3.select(_, DataForm.clockTimeChoices)),
            form3.group(form("clockIncrement"), raw("Clock increment"), half = true)(form3.select(_, DataForm.clockIncrementChoices))
          ),
          form3.split(
            form3.group(form("minutes"), trans.duration(), half = true)(form3.select(_, DataForm.minuteChoices)),
            form3.group(form("waitMinutes"), trans.timeBeforeTournamentStarts(), half = true)(form3.select(_, DataForm.waitMinuteChoices))
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
              a(cls := "show")(trans.showAdvancedSettings())
            ),
            div(cls := "form")(
              form3.group(form("password"), trans.password(), help = raw("Make the tournament private, and restrict access with a password").some)(form3.input(_)),
              condition(form, auto = true, teams = teams),
              input(tpe := "hidden", name := form("berserkable").name, value := "false"), // hack allow disabling berserk
              form3.group(form("startDate"), raw("Custom start date"), help = raw("""This overrides the "Time before tournament starts" setting""").some)(form3.flatpickr(_))
            )
          ),
          form3.actions(
            a(href := routes.Tournament.home())(trans.cancel()),
            form3.submit(trans.createANewTournament(), icon = "g".some)
          )
        )
      ),
      div(cls := "box box-pad tour__faq")(tournament.faq())
    ))

  private def autoField(auto: Boolean, field: Field)(visible: Field => Frag) = frag(
    if (auto) form3.hidden(field) else visible(field)
  )

  def condition(form: Form[_], auto: Boolean, teams: lila.hub.lightTeam.TeamIdsWithNames)(implicit ctx: Context) = frag(
    form3.split(
      form3.group(form("conditions.nbRatedGame.nb"), raw("Minimum rated games"), half = true)(form3.select(_, Condition.DataForm.nbRatedGameChoices)),
      autoField(auto, form("conditions.nbRatedGame.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, ("", "Any") :: Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      form3.group(form("conditions.minRating.rating"), raw("Minimum rating"), half = true)(form3.select(_, Condition.DataForm.minRatingChoices)),
      autoField(auto, form("conditions.minRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      form3.group(form("conditions.maxRating.rating"), raw("Maximum weekly rating"), half = true)(form3.select(_, Condition.DataForm.maxRatingChoices)),
      autoField(auto, form("conditions.maxRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) ?? {
        form3.checkbox(form("conditions.titled"), raw("Only titled players"), help = raw("Require an official title to join the tournament").some, half = true)
      },
      form3.checkbox(form("berserkable"), raw("Allow Berserk"), help = raw("Let players halve their clock time to gain an extra point").some, half = true)
    ),
    (auto && teams.size > 0) ?? {
      form3.group(form("conditions.teamMember.teamId"), raw("Only members of team"), half = false)(form3.select(_, List(("", "No Restriction")) ::: teams))
    }
  )

  def startingPosition(field: Field)(implicit ctx: Context) = st.select(
    id := form3.id(field),
    name := field.name,
    cls := "form-control"
  )(
      option(
        value := chess.StartingPosition.initial.fen,
        field.value.has(chess.StartingPosition.initial.fen) option selected
      )(chess.StartingPosition.initial.name),
      chess.StartingPosition.categories.map { categ =>
        optgroup(attr("label") := categ.name)(
          categ.positions.map { v =>
            option(value := v.fen, field.value.has(v.fen) option selected)(v.fullName)
          }
        )
      }
    )
}
