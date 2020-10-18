package views.html
package tournament

import controllers.routes
import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.hub.LeaderTeam
import lila.tournament.{ Condition, Tournament, TournamentForm }

object form {

  def create(form: Form[_], leaderTeams: List[LeaderTeam])(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.newTournament.txt(),
      moreCss = cssTag("tournament.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = new TourFields(form, none)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(
            if (fields.isTeamBattle) "New Team Battle"
            else trans.createANewTournament()
          ),
          postForm(cls := "form3", action := routes.Tournament.create())(
            fields.name,
            form3.split(fields.rated, fields.variant),
            fields.clock,
            form3.split(fields.minutes, fields.waitMinutes),
            form3.split(fields.description(true), fields.startPosition),
            form3.globalError(form),
            fieldset(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                condition(form, fields, auto = true, teams = leaderTeams, tour = none),
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

  def edit(tour: Tournament, form: Form[_], myTeams: List[LeaderTeam])(implicit ctx: Context) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = new TourFields(form, tour.some)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1("Edit ", tour.name()),
          postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
            form3.split(fields.name, tour.isCreated option fields.startDate),
            form3.split(fields.rated, fields.variant),
            fields.startPosition,
            fields.clock,
            form3.split(
              if (TournamentForm.minutes contains tour.minutes) form3.split(fields.minutes)
              else
                form3.group(form("minutes"), trans.duration(), half = true)(
                  form3.input(_)(tpe := "number")
                )
            ),
            fields.description(true),
            form3.globalError(form),
            fieldset(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                condition(form, fields, auto = true, teams = myTeams, tour = tour.some)
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

  private def autoField(auto: Boolean, field: Field)(visible: Field => Frag) =
    frag(
      if (auto) form3.hidden(field) else visible(field)
    )

  def condition(
      form: Form[_],
      fields: TourFields,
      auto: Boolean,
      teams: List[LeaderTeam],
      tour: Option[Tournament]
  )(implicit
      ctx: Context
  ) =
    frag(
      form3.split(
        fields.password,
        (auto && tour.isEmpty && teams.nonEmpty) option {
          val baseField = form("conditions.teamMember.teamId")
          val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
            case None       => baseField
            case Some(team) => baseField.copy(value = team.some)
          }
          form3.group(field, frag("Only members of team"), half = true)(
            form3.select(_, List(("", "No Restriction")) ::: teams.map(_.pair))
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), frag("Minimum rated games"), half = true)(
          form3.select(_, Condition.DataForm.nbRatedGameChoices)
        ),
        autoField(auto, form("conditions.nbRatedGame.perf")) { field =>
          form3.group(field, frag("In variant"), half = true)(
            form3.select(_, ("", "Any") :: Condition.DataForm.perfChoices)
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), frag("Minimum rating"), half = true)(
          form3.select(_, Condition.DataForm.minRatingChoices)
        ),
        autoField(auto, form("conditions.minRating.perf")) { field =>
          form3.group(field, frag("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
        }
      ),
      form3.split(
        form3.group(form("conditions.maxRating.rating"), frag("Maximum weekly rating"), half = true)(
          form3.select(_, Condition.DataForm.maxRatingChoices)
        ),
        autoField(auto, form("conditions.maxRating.perf")) { field =>
          form3.group(field, frag("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
        }
      ),
      form3.split(
        (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) ?? {
          form3.checkbox(
            form("conditions.titled"),
            frag("Only titled players"),
            help = frag("Require an official title to join the tournament").some,
            half = true
          )
        },
        form3.checkbox(
          form("berserkable"),
          frag("Allow Berserk"),
          help = frag("Let players halve their clock time to gain an extra point").some,
          half = true
        ),
        form3.hidden(form("berserkable"), "false".some) // hack to allow disabling berserk
      ),
      form3.split(
        form3.checkbox(
          form("hasChat"),
          trans.chatRoom(),
          help = frag("Let players discuss in a chat room").some,
          half = true
        ),
        form3.hidden(form("hasChat"), "false".some), // hack to allow disabling chat
        form3.checkbox(
          form("streakable"),
          frag("Arena streaks"),
          help = frag("After 2 wins, consecutive wins grant 4 points instead of 2.").some,
          half = true
        ),
        form3.hidden(form("streakable"), "false".some) // hack to allow disabling streaks
      )
    )

  def startingPosition(field: Field, tour: Option[Tournament]) =
    form3.input(field)(
      tour.exists(t => !t.isCreated && t.position.initial).option(disabled := true)
    )
}

final private class TourFields(form: Form[_], tour: Option[Tournament])(implicit ctx: Context) {

  def isTeamBattle = tour.exists(_.isTeamBattle) || form("teamBattleByTeam").value.nonEmpty

  def disabledAfterStart = tour.exists(!_.isCreated)

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
      form3.select(
        _,
        translatedVariantChoicesWithVariants.map(x => x._1 -> x._2),
        disabled = disabledAfterStart
      )
    )
  def startPosition =
    form3.group(
      form("position"),
      trans.startPosition(),
      klass = "position",
      half = true,
      help = frag(
        "Paste a valid FEN to start every game from a given position.",
        br,
        "It only works for standard games, not with variants.",
        br,
        "You can use the ",
        a(href := routes.Editor.index(), target := "_blank")("board editor"),
        " to generate a FEN position, then paste it here.",
        br,
        "Leave empty to start games from the normal initial position."
      ).some
    )(
      views.html.tournament.form.startingPosition(_, tour)
    )
  def clock =
    form3.split(
      form3.group(form("clockTime"), trans.clockInitialTime(), half = true)(
        form3.select(_, TournamentForm.clockTimeChoices, disabled = disabledAfterStart)
      ),
      form3.group(form("clockIncrement"), trans.clockIncrement(), half = true)(
        form3.select(_, TournamentForm.clockIncrementChoices, disabled = disabledAfterStart)
      )
    )
  def minutes =
    form3.group(form("minutes"), trans.duration(), half = true)(
      form3.select(_, TournamentForm.minuteChoices)
    )
  def waitMinutes =
    form3.group(form("waitMinutes"), trans.timeBeforeTournamentStarts(), half = true)(
      form3.select(_, TournamentForm.waitMinuteChoices)
    )
  def description(half: Boolean) =
    form3.group(
      form("description"),
      frag("Tournament description"),
      help = frag(
        "Anything special you want to tell the participants? Try to keep it short. Markdown links are available: [name](https://url)"
      ).some,
      half = half
    )(form3.textarea(_)(rows := 4))
  def password =
    !isTeamBattle option
      form3.group(
        form("password"),
        trans.password(),
        help = trans.makePrivateTournament().some,
        half = true
      )(form3.input(_)(autocomplete := "off"))
  def startDate =
    form3.group(
      form("startDate"),
      frag("Custom start date"),
      help = frag(
        """In your own local timezone. This overrides the "Time before tournament starts" setting"""
      ).some
    )(form3.flatpickr(_))
  def advancedSettings =
    frag(
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
