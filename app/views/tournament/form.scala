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
            if (fields.isTeamBattle) trans.arena.newTeamBattle()
            else trans.createANewTournament()
          ),
          postForm(cls := "form3", action := routes.Tournament.create)(
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
              a(href := routes.Tournament.home)(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = "".some)
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
            fields.clock,
            form3.split(
              if (TournamentForm.minutes contains tour.minutes) form3.split(fields.minutes)
              else
                form3.group(form("minutes"), trans.duration(), half = true)(
                  form3.input(_)(tpe := "number")
                )
            ),
            form3.split(fields.description(true), fields.startPosition),
            form3.globalError(form),
            fieldset(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                condition(form, fields, auto = true, teams = myTeams, tour = tour.some)
              )
            ),
            form3.actions(
              a(href := routes.Tournament.show(tour.id))(trans.cancel()),
              form3.submit(trans.save(), icon = "".some)
            )
          ),
          postForm(cls := "terminate", action := routes.Tournament.terminate(tour.id))(
            submitButton(dataIcon := "", cls := "text button button-red confirm")(
              trans.cancelTournament()
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
        fields.entryCode,
        (auto && tour.isEmpty && teams.nonEmpty) option {
          val baseField = form("conditions.teamMember.teamId")
          val field = ctx.req.queryString get "team" flatMap (_.headOption) match {
            case None       => baseField
            case Some(team) => baseField.copy(value = team.some)
          }
          form3.group(field, trans.onlyMembersOfTeam(), half = true)(
            form3.select(_, List(("", trans.noRestriction.txt())) ::: teams.map(_.pair))
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), trans.minimumRatedGames(), half = true)(
          form3.select(_, Condition.DataForm.nbRatedGameChoices)
        ),
        autoField(auto, form("conditions.nbRatedGame.perf")) { field =>
          form3.group(field, frag("In variant"), half = true)(
            form3.select(_, ("", "Any") :: Condition.DataForm.perfChoices)
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), trans.minimumRating(), half = true)(
          form3.select(_, Condition.DataForm.minRatingChoices)
        ),
        autoField(auto, form("conditions.minRating.perf")) { field =>
          form3.group(field, frag("In variant"), half = true)(form3.select(_, Condition.DataForm.perfChoices))
        }
      ),
      form3.split(
        form3.group(form("conditions.maxRating.rating"), trans.maximumWeeklyRating(), half = true)(
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
            trans.onlyTitled(),
            help = trans.onlyTitledHelp().some,
            half = true
          )
        },
        form3.checkbox(
          form("berserkable"),
          trans.arena.allowBerserk(),
          help = trans.arena.allowBerserkHelp().some,
          half = true
        ),
        form3.hidden(form("berserkable"), "false".some) // hack to allow disabling berserk
      ),
      form3.split(
        form3.checkbox(
          form("hasChat"),
          trans.chatRoom(),
          help = trans.arena.allowChatHelp().some,
          half = true
        ),
        form3.hidden(form("hasChat"), "false".some), // hack to allow disabling chat
        form3.checkbox(
          form("streakable"),
          trans.arena.arenaStreaks(),
          help = trans.arena.arenaStreaksHelp().some,
          half = true
        ),
        form3.hidden(form("streakable"), "false".some) // hack to allow disabling streaks
      )
    )

  def startingPosition(field: Field, tour: Option[Tournament]) =
    form3.input(field)(
      tour.exists(t => !t.isCreated && t.position.isEmpty).option(disabled := true)
    )
}

final private class TourFields(form: Form[_], tour: Option[Tournament])(implicit ctx: Context) {

  def isTeamBattle = tour.exists(_.isTeamBattle) || form("teamBattleByTeam").value.nonEmpty

  private def disabledAfterStart = tour.exists(!_.isCreated)

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
        help = trans.ratedFormHelp().some
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
      help =
        trans.positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.boardEditor.txt())).some
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
      trans.tournDescription(),
      help = trans.tournDescriptionHelp().some,
      half = half
    )(form3.textarea(_)(rows := 4))
  def entryCode =
    form3.group(
      form("password"),
      trans.tournamentEntryCode(),
      help = trans.makePrivateTournamentHelp().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))
  def startDate =
    form3.group(
      form("startDate"),
      trans.arena.customStartDate(),
      help = trans.arena.customStartDateHelp().some
    )(form3.flatpickr(_))
  def advancedSettings =
    frag(
      legend(trans.advancedSettings()),
      errMsg(form("conditions")),
      p(
        strong(dataIcon := "", cls := "text")(trans.recommendNotTouching()),
        " ",
        trans.fewerPlayers(),
        " ",
        a(cls := "show")(trans.showAdvancedSettings())
      )
    )
}
