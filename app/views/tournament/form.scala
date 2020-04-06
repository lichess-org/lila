package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.{ Condition, DataForm, Tournament }

import controllers.routes

object form {

  def create(form: Form[_], myTeams: List[lila.hub.LightTeam])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.newTournament.txt(),
      moreCss = cssTag("tournament.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
      val fields = new TourFields(form)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(
            if (fields.isTeamBattle) "New Team Battle"
            else trans.createANewTournament()
          ),
          postForm(cls := "form3", action := routes.Tournament.create)(
            fields.name,
            form3.split(fields.rated, fields.variant),
            fields.startPosition,
            fields.clock,
            form3.split(fields.minutes, fields.waitMinutes),
            fields.description,
            form3.globalError(form),
            fieldset(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                fields.password,
                condition(form, auto = true, teams = myTeams),
                fields.startDate
              )
            ),
            fields.isTeamBattle option form3.hidden(form("teamBattleByTeam")),
            form3.actions(
              a(href := routes.Tournament.home())(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = "g".some)
            )
          )
        ),
        div(cls := "box box-pad tour__faq")(tournament.faq())
      )
    }

  def edit(tour: Tournament, form: Form[_], myTeams: List[lila.hub.LightTeam])(implicit ctx: Context) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      moreJs = frag(
        flatpickrTag,
        jsTag("tournamentForm.js")
      )
    ) {
      val fields = new TourFields(form)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1("Edit ", tour.name()),
          postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
            form3.split(fields.name, fields.startDate),
            form3.split(fields.rated, fields.variant),
            fields.startPosition,
            fields.clock,
            form3.split(
              if (DataForm.minutes contains tour.minutes) form3.split(fields.minutes)
              else
                form3.group(form("minutes"), trans.duration(), half = true)(
                  form3.input(_)(tpe := "number")
                )
            ),
            fields.description,
            form3.globalError(form),
            fieldset(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                fields.password,
                condition(form, auto = true, teams = myTeams)
              )
            ),
            form3.actions(
              a(href := routes.Tournament.show(tour.id))(trans.cancel()),
              form3.submit(trans.save(), icon = "g".some)
            )
          ),
          postForm(cls := "terminate", action := routes.Tournament.terminate(tour.id))(
            submitButton(dataIcon := "j", cls := "text button button-red confirm")(
              "Cancel the tournament"
            )
          )
        )
      )
    }

  private def autoField(auto: Boolean, field: Field)(visible: Field => Frag) = frag(
    if (auto) form3.hidden(field) else visible(field)
  )

  def condition(form: Form[_], auto: Boolean, teams: List[lila.hub.LightTeam])(implicit ctx: Context) = frag(
    form3.split(
      form3.group(form("conditions.nbRatedGame.nb"), raw("Minimum rated games"), half = true)(
        form3.select(_, Condition.DataForm.nbRatedGameChoices)
      ),
      autoField(auto, form("conditions.nbRatedGame.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(
          form3.select(_, ("", "Any") :: Condition.DataForm.perfChoices)
        )
      }
    ),
    form3.split(
      form3.group(form("conditions.minRating.rating"), raw("Minimum rating"), half = true)(
        form3.select(_, Condition.DataForm.minRatingChoices)
      ),
      autoField(auto, form("conditions.minRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      form3.group(form("conditions.maxRating.rating"), raw("Maximum weekly rating"), half = true)(
        form3.select(_, Condition.DataForm.maxRatingChoices)
      ),
      autoField(auto, form("conditions.maxRating.perf")) { field =>
        form3.group(field, raw("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
      }
    ),
    form3.split(
      (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) ?? {
        form3.checkbox(
          form("conditions.titled"),
          raw("Only titled players"),
          help = raw("Require an official title to join the tournament").some,
          half = true
        )
      },
      form3.checkbox(
        form("berserkable"),
        raw("Allow Berserk"),
        help = raw("Let players halve their clock time to gain an extra point").some,
        half = true
      ),
      input(tpe := "hidden", st.name := form("berserkable").name, value := "false") // hack allow disabling berserk
    ),
    (auto && teams.size > 0) option {
      val baseField = form("conditions.teamMember.teamId")
      val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
        case None       => baseField
        case Some(team) => baseField.copy(value = team.some)
      }
      form3.group(field, raw("Only members of team"), half = false)(
        form3.select(_, List(("", "No Restriction")) ::: teams.map(_.pair))
      )
    }
  )

  def startingPosition(field: Field) =
    st.select(
      id := form3.id(field),
      st.name := field.name,
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

final private class TourFields(form: Form[_])(implicit ctx: Context) {

  val isTeamBattle = form("teamBattleByTeam").value.nonEmpty

  def name =
    form3.group(form("name"), trans.name()) { f =>
      div(
        form3.input(f),
        " ",
        if (isTeamBattle) "Team Battle" else "Arena",
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

  def rated = frag(
    form3.checkbox(
      form("rated"),
      trans.rated(),
      help = raw("Games are rated<br>and impact players ratings").some
    ),
    st.input(tpe := "hidden", st.name := form("rated").name, value := "false") // hack allow disabling rated
  )
  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(_, translatedVariantChoicesWithVariants.map(x => x._1 -> x._2))
    )
  def startPosition =
    form3.group(form("position"), trans.startPosition(), klass = "position")(
      views.html.tournament.form.startingPosition(_)
    )
  def clock =
    form3.split(
      form3.group(form("clockTime"), trans.clockInitialTime(), half = true)(
        form3.select(_, DataForm.clockTimeChoices)
      ),
      form3.group(form("clockIncrement"), trans.clockIncrement(), half = true)(
        form3.select(_, DataForm.clockIncrementChoices)
      )
    )
  def minutes =
    form3.group(form("minutes"), trans.duration(), half = true)(
      form3.select(_, DataForm.minuteChoices)
    )
  def waitMinutes =
    form3.group(form("waitMinutes"), trans.timeBeforeTournamentStarts(), half = true)(
      form3.select(_, DataForm.waitMinuteChoices)
    )
  def description =
    form3.group(
      form("description"),
      raw("Tournament description"),
      help = frag("Anything special you want to tell the participants? Try to keep it short.").some
    )(form3.textarea(_)(rows := 2))
  def password =
    !isTeamBattle option
      form3.group(
        form("password"),
        trans.password(),
        help = trans.makePrivateTournament().some
      )(form3.input(_))
  def startDate =
    form3.group(
      form("startDate"),
      raw("Custom start date"),
      help = raw("""This overrides the "Time before tournament starts" setting""").some
    )(form3.flatpickr(_))
  def advancedSettings = frag(
    legend(trans.advancedSettings()),
    errMsg(form("conditions")),
    p(
      strong(dataIcon := "!", cls := "text")(trans.recommendNotTouching()),
      " ",
      trans.fewerPlayers(),
      " ",
      a(cls := "show")(trans.showAdvancedSettings())
    )
  )
}
