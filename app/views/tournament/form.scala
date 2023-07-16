package views.html
package tournament

import controllers.routes
import play.api.data.{ Field, Form }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.hub.LeaderTeam
import lila.tournament.{ Tournament, TournamentForm }
import lila.gathering.{ ConditionForm, GatheringClock }

object form:

  def create(form: Form[?], leaderTeams: List[LeaderTeam])(using PageContext) =
    views.html.base.layout(
      title = trans.newTournament.txt(),
      moreCss = cssTag("tournament.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = TourFields(form, none)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(cls := "box__top")(
            if fields.isTeamBattle then trans.arena.newTeamBattle()
            else trans.createANewTournament()
          ),
          postForm(cls := "form3", action := routes.Tournament.webCreate)(
            div(cls := "form-group")(
              a(
                dataIcon := licon.InfoCircle,
                cls      := "text",
                href     := routes.ContentPage.loneBookmark("event-tips")
              )(trans.ourEventTips())
            ),
            form3.globalError(form),
            fields.name,
            form3.split(fields.rated, fields.variant),
            fields.clock,
            form3.split(fields.minutes, fields.waitMinutes),
            form3.split(fields.description(true), fields.startPosition),
            form3.fieldset(trans.advancedSettings())(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                conditionFields(form, fields, teams = leaderTeams, tour = none),
                fields.startDate
              )
            ),
            fields.isTeamBattle option form3.hidden(form("teamBattleByTeam")),
            form3.actions(
              a(href := routes.Tournament.home)(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = licon.Trophy.some)
            )
          )
        ),
        div(cls := "box box-pad tour__faq")(tournament.faq())
      )
    }

  def edit(tour: Tournament, form: Form[?], myTeams: List[LeaderTeam])(using PageContext) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = TourFields(form, tour.some)
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(cls := "box__top")("Edit ", tour.name()),
          postForm(cls := "form3", action := routes.Tournament.update(tour.id))(
            form3.split(fields.name, tour.isCreated option fields.startDate),
            form3.split(fields.rated, fields.variant),
            fields.clock,
            form3.split(
              if TournamentForm.minutes contains tour.minutes then form3.split(fields.minutes)
              else
                form3.group(form("minutes"), trans.duration(), half = true)(
                  form3.input(_)(tpe := "number")
                )
            ),
            form3.split(fields.description(true), fields.startPosition),
            form3.globalError(form),
            form3.fieldset(trans.advancedSettings())(cls := "conditions")(
              fields.advancedSettings,
              div(cls := "form")(
                conditionFields(form, fields, teams = myTeams, tour = tour.some)
              )
            ),
            form3.actions(
              a(href := routes.Tournament.show(tour.id))(trans.cancel()),
              form3.submit(trans.save(), icon = licon.Trophy.some)
            )
          ),
          hr,
          br,
          br,
          postForm(cls := "terminate", action := routes.Tournament.terminate(tour.id))(
            submitButton(dataIcon := licon.CautionCircle, cls := "text button button-red confirm")(
              trans.cancelTournament()
            )
          )
        )
      )
    }

  def conditionFields(
      form: Form[?],
      fields: TourFields,
      teams: List[LeaderTeam],
      tour: Option[Tournament]
  )(using ctx: PageContext) =
    frag(
      form3.split(
        fields.entryCode,
        tour.isEmpty && teams.nonEmpty option {
          val baseField = form("conditions.teamMember.teamId")
          val field = ctx.req.queryString
            .get("team")
            .flatMap(_.headOption)
            .foldLeft(baseField): (field, team) =>
              field.copy(value = team.some)
          form3.group(field, trans.onlyMembersOfTeam(), half = true):
            form3.select(_, List(("", trans.noRestriction.txt())) ::: teams.map(_.pair))
        }
      ),
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), trans.minimumRatedGames(), half = true):
          form3.select(_, ConditionForm.nbRatedGameChoices)
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), trans.minimumRating(), half = true):
          form3.select(_, ConditionForm.minRatingChoices)
        ,
        form3.group(form("conditions.maxRating.rating"), trans.maximumWeeklyRating(), half = true):
          form3.select(_, ConditionForm.maxRatingChoices)
      ),
      form3.split(
        form3.group(
          form("conditions.allowList"),
          trans.swiss.predefinedUsers(),
          help = trans.swiss.forbiddedUsers().some,
          half = true
        )(form3.textarea(_)(rows := 4))
      ),
      form3.split(
        (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) so {
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
        form3.hiddenFalse(form("berserkable"))
      ),
      form3.split(
        form3.checkbox(
          form("hasChat"),
          trans.chatRoom(),
          help = trans.arena.allowChatHelp().some,
          half = true
        ),
        form3.hiddenFalse(form("hasChat")),
        form3.checkbox(
          form("streakable"),
          trans.arena.arenaStreaks(),
          help = trans.arena.arenaStreaksHelp().some,
          half = true
        ),
        form3.hiddenFalse(form("streakable"))
      )
    )

  def startingPosition(field: Field, tour: Option[Tournament]) =
    form3.input(field)(
      tour.exists(t => !t.isCreated && t.position.isEmpty).option(disabled := true)
    )

final private class TourFields(form: Form[?], tour: Option[Tournament])(using PageContext):

  def isTeamBattle = tour.exists(_.isTeamBattle) || form("teamBattleByTeam").value.nonEmpty

  private def disabledAfterStart = tour.exists(!_.isCreated)

  def name =
    form3.group(form("name"), trans.name()) { f =>
      div(
        form3.input(f),
        " ",
        if isTeamBattle then "Team Battle" else "Arena",
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
      form3.hiddenFalse(form("rated"))
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
        form3.select(_, GatheringClock.timeChoices, disabled = disabledAfterStart)
      ),
      form3.group(form("clockIncrement"), trans.clockIncrement(), half = true)(
        form3.select(_, GatheringClock.incrementChoices, disabled = disabledAfterStart)
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
      help = trans.makePrivateTournament().some,
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
      errMsg(form("conditions")),
      p(
        strong(dataIcon := licon.CautionTriangle, cls := "text")(trans.recommendNotTouching()),
        " ",
        trans.fewerPlayers(),
        " ",
        a(cls := "show")(trans.showAdvancedSettings())
      )
    )
