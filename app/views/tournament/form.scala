package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.{ Condition, DataForm, TimeControl, Tournament }

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
      val fields = new TourFields(form, none)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(
            if (fields.isTeamBattle) "New Team Battle"
            else trans.createANewTournament()
          ),
          postForm(cls := "form3 f-rt f-robin", action := routes.Tournament.create)(
            form3.globalError(form),
            allFieldsets(form, fields, teams = myTeams, tour = none),
            form3.actions(
              a(href := routes.Tournament.home)(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = "g".some)
            )
          )
        ),
        a(href := routes.Tournament.help(none))
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
      val fields = new TourFields(form, tour.some)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1("Edit ", tour.name()),
          postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
            form3.globalError(form),
            allFieldsets(form, fields, teams = myTeams, tour = tour.some),
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

  def allFieldsets(
      form: Form[?],
      fields: TourFields,
      teams: List[lila.hub.LightTeam],
      tour: Option[Tournament]
  )(implicit ctx: Context) =
    frag(
      tourFields(fields),
      gameFields(fields),
      clocksFields(form, fields),
      conditionFields(form, fields, teams = teams, tour = tour),
      featuresFields(form),
      fields.isTeamBattle option form3.hidden(form("teamBattleByTeam"))
    )

  def tourFields(
      fields: TourFields
  )(implicit ctx: Context) =
    form3.fieldset(trans.tournament.txt(), toggle = true.some)(
      form3.split(fields.name, fields.format),
      form3.split(fields.startDate, fields.finishDate),
      form3.split(fields.description)
    )

  def gameFields(
      fields: TourFields
  )(implicit ctx: Context) =
    form3.fieldset(trans.games.txt(), toggle = true.some)(
      form3.split(fields.variant, fields.startPosition)
    )

  def clocksFields(
      form: Form[?],
      fields: TourFields
  )(implicit ctx: Context) =
    form3.fieldset(trans.clock.txt(), toggle = true.some)(
      errMsg(form("timeControlSetup")),
      fields.clocks
    )

  def conditionFields(
      form: Form[?],
      fields: TourFields,
      teams: List[lila.hub.LightTeam],
      tour: Option[Tournament]
  )(implicit ctx: Context) =
    form3.fieldset("Entry conditions", toggle = tour.exists(_.conditions.list.nonEmpty).some)(
      errMsg(form("conditions")),
      form3.split(
        form3.checkbox(
          form("candidatesOnly"),
          frag("Request to join"),
          help = frag("Players can only join after you approve their request").some,
          half = true,
          disabled = fields.isTeamBattle
        ),
        fields.password
      ),
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), frag("Minimum rated games"), half = true)(
          form3.select(_, Condition.DataForm.nbRatedGameChoices)
        ),
        (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) ?? {
          form3.checkbox(
            form("conditions.titled"),
            frag("Only titled players"),
            help = frag("Require an official title to join the tournament").some,
            half = true
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), frag("Minimum rating"), half = true)(
          form3.select(_, Condition.DataForm.minRatingChoices)
        ),
        form3.group(form("conditions.maxRating.rating"), frag("Maximum weekly rating"), half = true)(
          form3.select(_, Condition.DataForm.maxRatingChoices)
        )
      ),
      (tour.isEmpty && teams.nonEmpty) option {
        val baseField = form("conditions.teamMember.teamId")
        val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
          case None       => baseField
          case Some(team) => baseField.copy(value = team.some)
        }
        form3.group(field, frag("Only members of team"), half = true)(
          form3.select(_, List(("", "No Restriction")) ::: teams.map(_.pair))
        )
      }
    )

  def featuresFields(form: Form[?])(implicit ctx: Context) =
    form3.fieldset(trans.features.txt(), toggle = false.some)(
      form3.split(
        div(cls := "f-arena")(
          form3.checkbox(
            form("berserkable"),
            trans.arena.berserk(),
            help = frag("Let players halve their clock time to gain an extra point").some,
            half = true
          ),
          form3.hiddenFalse(form("berserkable")),
          form3.checkbox(
            form("streakable"),
            "Streaks", // trans.arena.arenaStreaks(),
            help = frag("After 2 wins, consecutive wins grant 4 points instead of 2.").some,
            half = true
          ),
          form3.hiddenFalse(form("streakable"))
        )
      ),
      form3.split(
        form3.checkbox(
          form("rated"),
          trans.rated(),
          help = raw("Games are rated<br>and impact players ratings").some
        ),
        form3.hiddenFalse(form("rated")),
        form3.checkbox(
          form("hasChat"),
          trans.chatRoom(),
          help = frag("Let players discuss in a chat room").some,
          half = true
        ),
        form3.hiddenFalse(form("hasChat"))
      )
    )

  def startingPosition(field: Field, tour: Option[Tournament]) =
    form3.input(field)(
      tour.exists(t => !t.isCreated && t.position.isEmpty).option(disabled := true)
    )

  val positionInputHelp = frag(
    "Paste a valid SFEN to start every game from a given position.",
    br,
    "You can use the ",
    a(href := routes.Editor.index, target := "_blank")("board editor"),
    " to generate a SFEN position, then paste it here.",
    br,
    "Leave empty to start games from the normal initial position."
  )
}

final private class TourFields(form: Form[_], tour: Option[Tournament])(implicit ctx: Context) {

  def isTeamBattle = tour.exists(_.isTeamBattle) || form("teamBattleByTeam").value.nonEmpty

  private def disabledAfterStart  = tour.exists(!_.isCreated)
  private def disabledAfterCreate = tour.isDefined

  def format =
    form3.group(
      form("format"),
      "Format",
      half = true,
      help = frag(
        a(href := routes.Tournament.help(none))("More about formats")
      ).some
    )(
      form3.select(
        _,
        DataForm.formats.map(f => (f, transKeyTxt(f))),
        disabled = disabledAfterCreate
      )
    )

  def name =
    form3.group(
      form("name"),
      trans.name(),
      half = true,
      help = frag(
        trans.safeTournamentName(),
        br,
        trans.inappropriateNameWarning(),
        br,
        trans.emptyTournamentAnimalName()
      ).some
    ) { f =>
      form3.input(f)
    }

  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(
        _,
        DataForm.validVariants.map(v => (v.id.toString, transKeyTxt(v.key))),
        disabled = disabledAfterCreate
      )
    )

  def startPosition =
    form3.group(
      form("position"),
      trans.startPosition(),
      klass = "position",
      half = true,
      help = tournament.form.positionInputHelp.some
    )(
      views.html.tournament.form.startingPosition(_, tour)
    )

  def clocks =
    frag(
      form3.split(
        form3.group(form("timeControlSetup.timeControl"), trans.timeControl(), half = true)(
          form3.select(
            _,
            TimeControl.DataForm.timeControls.map(tc =>
              (tc, if (tc == TimeControl.RealTime.id) trans.realTime.txt() else trans.correspondence.txt())
            ),
            disabled = disabledAfterCreate
          )
        ),
        form3
          .group(form("timeControlSetup.daysPerTurn"), trans.daysPerTurn(), klass = "f-corres", half = true)(
            form3.select(
              _,
              TimeControl.DataForm.daysPerTurn.map(d => (d, trans.nbDays.pluralSameTxt(d))),
              disabled = disabledAfterStart
            )
          ),
        form3.group(
          form("timeControlSetup.clockTime"),
          trans.clockInitialTime(),
          klass = "f-rt",
          half = true
        )(
          form3.select(
            _,
            TimeControl.DataForm.clockTimes.map { ct =>
              if (ct < 1) (ct, trans.nbSeconds.pluralSameTxt(ct * 60 toInt))
              else (ct, trans.nbMinutes.pluralSameTxt(ct.toInt))
            },
            disabled = disabledAfterStart
          )
        )
      ),
      form3.split(
        form3.group(form("timeControlSetup.clockByoyomi"), trans.clockByoyomi(), klass = "f-rt", half = true)(
          form3.select(
            _,
            TimeControl.DataForm.clockByoyomi.map(b => (b, trans.nbSeconds.pluralSameTxt(b))),
            disabled = disabledAfterStart
          )
        ),
        form3.group(form("timeControlSetup.periods"), trans.numberOfPeriods(), klass = "f-rt", half = true)(
          form3.select(
            _,
            TimeControl.DataForm.periods.map(p => (p, p.toString)),
            disabled = disabledAfterStart
          )
        )
      ),
      form3.split(
        form3.group(
          form("timeControlSetup.clockIncrement"),
          trans.clockIncrement(),
          klass = "f-rt",
          half = true
        )(
          form3.select(
            _,
            TimeControl.DataForm.clockIncrements.map(i => (i, trans.nbSeconds.pluralSameTxt(i))),
            disabled = disabledAfterStart
          )
        )
      )
    )

  def startDate =
    form3.group(
      form("startDate"),
      frag("Start date"),
      half = true,
      help = frag("Leave empty to start now").some // tournament.form.positionInputHelp.some
    )(form3.flatpickr(_))

  def finishDate =
    frag(
      form3.group(form("minutes"), trans.duration(), klass = "f-arena", half = true)(
        form3.select(_, DataForm.minutes.map(m => (m, trans.nbMinutes.pluralSameTxt(m))))
      ),
      form3.group(
        form("finishDate"),
        frag("End date"),
        klass = "f-robin",
        half = true
      )(form3.flatpickr(_))
    )

  def description =
    form3.group(
      form("description"),
      frag("Tournament description"),
      help = frag(
        "Anything special you want to tell the participants? Try to keep it short."
      ).some
    )(form3.textarea(_)(rows := 2))

  def password =
    form3.group(
      form("password"),
      trans.password(),
      help = trans.makePrivateTournament().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))

}
