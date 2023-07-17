package views.html.swiss

import controllers.routes
import play.api.data.Form

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.swiss.{ Swiss, SwissForm }
import lila.gathering.{ ConditionForm, GatheringClock }

object form:

  def create(form: Form[SwissForm.SwissData], teamId: TeamId)(using PageContext) =
    views.html.base.layout(
      title = trans.swiss.newSwiss.txt(),
      moreCss = cssTag("swiss.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = new SwissFields(form, none)
      main(cls := "page-small")(
        div(cls := "swiss__form tour__form box box-pad")(
          h1(cls := "box__top")(trans.swiss.newSwiss()),
          postForm(cls := "form3", action := routes.Swiss.create(teamId))(
            div(cls := "form-group")(
              a(
                dataIcon := licon.InfoCircle,
                cls      := "text",
                href     := routes.ContentPage.loneBookmark("event-tips")
              )(
                trans.ourEventTips()
              )
            ),
            form3.split(fields.name, fields.nbRounds),
            form3.split(fields.description, fields.rated),
            fields.clock,
            form3.split(fields.roundInterval, fields.startsAt),
            advancedSettings(
              form3.split(fields.variant, fields.position),
              form3.split(fields.chatFor, fields.entryCode),
              condition(form),
              form3.split(fields.playYourGames, fields.allowList),
              form3.split(fields.forbiddenPairings, fields.manualPairings)
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Team.show(teamId))(trans.cancel()),
              form3.submit(trans.createANewTournament(), icon = licon.Trophy.some)
            )
          )
        )
      )
    }

  def edit(swiss: Swiss, form: Form[SwissForm.SwissData])(using PageContext) =
    views.html.base.layout(
      title = swiss.name,
      moreCss = cssTag("swiss.form"),
      moreJs = jsModule("tourForm")
    ) {
      val fields = new SwissFields(form, swiss.some)
      main(cls := "page-small")(
        div(cls := "swiss__form box box-pad")(
          h1(cls := "box__top")("Edit ", swiss.name),
          postForm(cls := "form3", action := routes.Swiss.update(swiss.id))(
            form3.split(fields.name, fields.nbRounds),
            form3.split(fields.description, fields.rated),
            fields.clock,
            form3.split(fields.roundInterval, swiss.isCreated option fields.startsAt),
            advancedSettings(
              form3.split(fields.variant, fields.position),
              form3.split(fields.chatFor, fields.entryCode),
              condition(form),
              form3.split(fields.playYourGames, fields.allowList),
              form3.split(fields.forbiddenPairings, fields.manualPairings)
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Swiss.show(swiss.id))(trans.cancel()),
              form3.submit(trans.save(), icon = licon.Trophy.some)
            )
          ),
          postForm(cls := "terminate", action := routes.Swiss.terminate(swiss.id))(
            submitButton(dataIcon := licon.CautionCircle, cls := "text button button-red confirm")(
              trans.cancelTournament()
            )
          )
        )
      )
    }

  private def advancedSettings(settings: Frag*) =
    details(summary("Advanced settings"), settings)

  private def condition(form: Form[SwissForm.SwissData])(using ctx: PageContext) =
    frag(
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), trans.minimumRatedGames(), half = true)(
          form3.select(_, ConditionForm.nbRatedGameChoices)
        ),
        (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)) so {
          form3.checkbox(
            form("conditions.titled"),
            trans.onlyTitled(),
            help = trans.onlyTitledHelp().some,
            half = true
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), trans.minimumRating(), half = true)(
          form3.select(_, ConditionForm.minRatingChoices)
        ),
        form3.group(form("conditions.maxRating.rating"), trans.maximumWeeklyRating(), half = true)(
          form3.select(_, ConditionForm.maxRatingChoices)
        )
      )
    )

final private class SwissFields(form: Form[SwissForm.SwissData], swiss: Option[Swiss])(using PageContext):

  private def disabledAfterStart = swiss.exists(!_.isCreated)

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
      trans.swiss.numberOfRounds(),
      help = trans.swiss.numberOfRoundsHelp().some,
      half = true
    )(
      form3.input(_, typ = "number")
    )

  def rated =
    frag(
      form3.checkbox(
        form("rated"),
        trans.rated(),
        help = trans.ratedFormHelp().some,
        half = true
      ),
      form3.hidden(form("rated"), "false".some) // hack allow disabling rated
    )
  def variant =
    form3.group(form("variant"), trans.variant(), half = true)(
      form3.select(
        _,
        translatedVariantChoicesWithVariants(_.key).map(x => x._1 -> x._2),
        disabled = disabledAfterStart
      )
    )
  def clock =
    form3.split(
      form3.group(form("clock.limit"), trans.clockInitialTime(), half = true)(
        form3.select(_, SwissForm.clockLimitChoices, disabled = disabledAfterStart)
      ),
      form3.group(form("clock.increment"), trans.clockIncrement(), half = true)(
        form3.select(_, GatheringClock.incrementChoices, disabled = disabledAfterStart)
      )
    )
  def roundInterval =
    form3.group(form("roundInterval"), trans.swiss.roundInterval(), half = true)(
      form3.select(_, SwissForm.roundIntervalChoices)
    )
  def description =
    form3.group(
      form("description"),
      trans.tournDescription(),
      help = trans.tournDescriptionHelp().some,
      half = true
    )(form3.textarea(_)(rows := 4))
  def position =
    form3.group(
      form("position"),
      trans.startPosition(),
      klass = "position",
      half = true,
      help =
        trans.positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.boardEditor.txt())).some
    )(form3.input(_))
  def startsAt =
    form3.group(
      form("startsAt"),
      trans.swiss.tournStartDate(),
      help = trans.inYourLocalTimezone().some,
      half = true
    )(form3.flatpickr(_))

  def chatFor =
    form3.group(form("chatFor"), trans.tournChat(), half = true) { f =>
      form3.select(
        f,
        Seq(
          Swiss.ChatFor.NONE    -> trans.noChat.txt(),
          Swiss.ChatFor.LEADERS -> trans.onlyTeamLeaders.txt(),
          Swiss.ChatFor.MEMBERS -> trans.onlyTeamMembers.txt(),
          Swiss.ChatFor.ALL     -> trans.study.everyone.txt()
        )
      )
    }

  def entryCode =
    form3.group(
      form("password"),
      trans.tournamentEntryCode(),
      help = trans.makePrivateTournament().some,
      half = true
    )(form3.input(_)(autocomplete := "off"))

  def forbiddenPairings =
    form3.group(
      form("forbiddenPairings"),
      trans.swiss.forbiddenPairings(),
      help = trans.swiss.forbiddenPairingsHelp().some,
      half = true
    )(form3.textarea(_)(rows := 4))

  def manualPairings =
    form3.group(
      form("manualPairings"),
      "Manual pairings in next round",
      help = frag(
        "Specify all pairings of the next round manually. One player pair per line. Example:",
        br,
        "PlayerA PlayerB",
        br,
        "PlayerC PlayerD",
        br,
        "To give a bye (1 point) to a player instead of a pairing, add a line like so:",
        br,
        "PlayerE 1",
        br,
        "Missing players will be considered absent and get zero points.",
        br,
        "Leave this field empty to let lichess create pairings automatically."
      ).some,
      half = true
    )(form3.textarea(_)(rows := 4))

  def allowList = form3.group(
    form("conditions.allowList"),
    trans.swiss.predefinedUsers(),
    help = trans.swiss.forbiddedUsers().some,
    half = true
  )(form3.textarea(_)(rows := 4))

  def playYourGames = frag(
    form3.checkbox(
      form("conditions.playYourGames"),
      "Must have played their last swiss game",
      help = frag(
        "Only let players join if they have played their last swiss game. If they failed to show up in a recent swiss event, they won't be able to enter yours. This results in a better swiss experience for the players who actually show up."
      ).some,
      half = true
    ),
    form3.hiddenFalse(form("conditions.playYourGames"))
  )
