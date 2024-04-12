package views.html.swiss

import controllers.routes
import controllers.team.routes.Team as teamRoutes
import play.api.data.Form

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.gathering.{ ConditionForm, GatheringClock }
import lila.swiss.{ Swiss, SwissForm }

object form:

  def create(form: Form[SwissForm.SwissData], teamId: TeamId)(using PageContext) =
    views.html.base.layout(
      title = trans.swiss.newSwiss.txt(),
      moreCss = cssTag("swiss.form"),
      modules = jsModule("bits.tourForm")
    ) {
      val fields = new SwissFields(form, none)
      main(cls := "page-small")(
        div(cls := "swiss__form tour__form box box-pad")(
          h1(cls := "box__top")(trans.swiss.newSwiss()),
          postForm(cls := "form3", action := routes.Swiss.create(teamId))(
            div(cls := "form-group")(
              a(
                dataIcon := Icon.InfoCircle,
                cls      := "text",
                href     := routes.Cms.lonePage("event-tips")
              )(
                trans.site.ourEventTips()
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
              a(href := teamRoutes.show(teamId))(trans.site.cancel()),
              form3.submit(trans.site.createANewTournament(), icon = Icon.Trophy.some)
            )
          )
        )
      )
    }

  def edit(swiss: Swiss, form: Form[SwissForm.SwissData])(using PageContext) =
    views.html.base.layout(
      title = swiss.name,
      moreCss = cssTag("swiss.form"),
      modules = jsModule("bits.tourForm")
    ) {
      val fields = new SwissFields(form, swiss.some)
      main(cls := "page-small")(
        div(cls := "swiss__form box box-pad")(
          h1(cls := "box__top")("Edit ", swiss.name),
          postForm(cls := "form3", action := routes.Swiss.update(swiss.id))(
            form3.split(fields.name, fields.nbRounds),
            form3.split(fields.description, fields.rated),
            fields.clock,
            form3.split(fields.roundInterval, swiss.isCreated.option(fields.startsAt)),
            advancedSettings(
              form3.split(fields.variant, fields.position),
              form3.split(fields.chatFor, fields.entryCode),
              condition(form),
              form3.split(fields.playYourGames, fields.allowList),
              form3.split(fields.forbiddenPairings, fields.manualPairings)
            ),
            form3.globalError(form),
            form3.actions(
              a(href := routes.Swiss.show(swiss.id))(trans.site.cancel()),
              form3.submit(trans.site.save(), icon = Icon.Trophy.some)
            )
          ),
          postForm(cls := "terminate", action := routes.Swiss.terminate(swiss.id))(
            submitButton(dataIcon := Icon.CautionCircle, cls := "text button button-red confirm")(
              trans.site.cancelTournament()
            )
          )
        )
      )
    }

  private def advancedSettings(settings: Frag*)(using Context) =
    details(summary(trans.site.advancedSettings()), settings)

  private def condition(form: Form[SwissForm.SwissData])(using ctx: PageContext) =
    frag(
      form3.split(
        form3.group(form("conditions.nbRatedGame.nb"), trans.site.minimumRatedGames(), half = true)(
          form3.select(_, ConditionForm.nbRatedGameChoices)
        ),
        (ctx.me.exists(_.hasTitle) || isGranted(_.ManageTournament)).so {
          form3.checkbox(
            form("conditions.titled"),
            trans.arena.onlyTitled(),
            help = trans.arena.onlyTitledHelp().some,
            half = true
          )
        }
      ),
      form3.split(
        form3.group(form("conditions.minRating.rating"), trans.site.minimumRating(), half = true)(
          form3.select(_, ConditionForm.minRatingChoices)
        ),
        form3.group(form("conditions.maxRating.rating"), trans.site.maximumWeeklyRating(), half = true)(
          form3.select(_, ConditionForm.maxRatingChoices)
        )
      )
    )

final private class SwissFields(form: Form[SwissForm.SwissData], swiss: Option[Swiss])(using PageContext):

  private def disabledAfterStart = swiss.exists(!_.isCreated)

  def name =
    form3.group(form("name"), trans.site.name()) { f =>
      div(
        form3.input(f),
        small(cls := "form-help")(
          trans.site.safeTournamentName(),
          br,
          trans.site.inappropriateNameWarning(),
          br,
          trans.site.emptyTournamentName()
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
        trans.site.rated(),
        help = trans.site.ratedFormHelp().some,
        half = true
      ),
      form3.hidden(form("rated"), "false".some) // hack allow disabling rated
    )
  def variant =
    form3.group(form("variant"), trans.site.variant(), half = true)(
      form3.select(
        _,
        translatedVariantChoicesWithVariants(_.key).map(x => x._1 -> x._2),
        disabled = disabledAfterStart
      )
    )
  def clock =
    form3.split(
      form3.group(form("clock.limit"), trans.site.clockInitialTime(), half = true)(
        form3.select(_, SwissForm.clockLimitChoices, disabled = disabledAfterStart)
      ),
      form3.group(form("clock.increment"), trans.site.clockIncrement(), half = true)(
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
      trans.site.tournDescription(),
      help = trans.site.tournDescriptionHelp().some,
      half = true
    )(form3.textarea(_)(rows := 4))
  def position =
    form3.group(
      form("position"),
      trans.site.startPosition(),
      klass = "position",
      half = true,
      help = trans.site
        .positionInputHelp(a(href := routes.Editor.index, targetBlank)(trans.site.boardEditor.txt()))
        .some
    )(form3.input(_))
  def startsAt =
    form3.group(
      form("startsAt"),
      trans.swiss.tournStartDate(),
      help = trans.site.inYourLocalTimezone().some,
      half = true
    )(form3.flatpickr(_))

  def chatFor =
    form3.group(form("chatFor"), trans.site.tournChat(), half = true) { f =>
      form3.select(
        f,
        Seq(
          Swiss.ChatFor.NONE    -> trans.site.noChat.txt(),
          Swiss.ChatFor.LEADERS -> trans.site.onlyTeamLeaders.txt(),
          Swiss.ChatFor.MEMBERS -> trans.site.onlyTeamMembers.txt(),
          Swiss.ChatFor.ALL     -> trans.study.everyone.txt()
        )
      )
    }

  def entryCode =
    form3.group(
      form("password"),
      trans.site.tournamentEntryCode(),
      help = trans.site.makePrivateTournament().some,
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
      trans.swiss.manualPairings(),
      help = trans.swiss.manualPairingsHelp().some,
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
      trans.swiss.mustHavePlayedTheirLastSwissGame(),
      help = trans.swiss.mustHavePlayedTheirLastSwissGameHelp().some,
      half = true
    ),
    form3.hiddenFalse(form("conditions.playYourGames"))
  )
