package lila.swiss
package ui

import chess.variant.Variant
import play.api.data.{ Field, Form }

import lila.core.i18n.Translate
import lila.gathering.GatheringClock
import lila.gathering.ui.GatheringFormUi
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SwissFormUi(helpers: Helpers)(
    translatedVariantChoicesWithVariants: (
        Variant => String
    ) => Translate ?=> List[(String, String, Option[String])]
):
  import helpers.{ *, given }

  def create(form: Form[SwissForm.SwissData], teamId: TeamId)(using Context) =
    Page(trans.swiss.newSwiss.txt())
      .css("swiss.form")
      .js(Esm("bits.tourForm")):
        val fields = new SwissFields(form, none)
        main(cls := "page-small")(
          div(cls := "swiss__form tour__form box box-pad")(
            h1(cls := "box__top")(trans.swiss.newSwiss()),
            postForm(cls := "form3", action := routes.Swiss.create(teamId))(
              div(cls := "form-group")(
                a(
                  dataIcon := Icon.InfoCircle,
                  cls := "text",
                  href := routes.Cms.lonePage(lila.core.id.CmsPageKey("event-tips"))
                )(
                  trans.site.ourEventTips()
                )
              ),
              fields.tournamentFields,
              fields.gameFields,
              fields.featuresFields,
              fields.conditionFields,
              fields.pairingsFields,
              form3.globalError(form),
              form3.actions(
                a(href := routes.Team.show(teamId))(trans.site.cancel()),
                form3.submit(trans.site.createANewTournament(), icon = Icon.Trophy.some)
              )
            )
          )
        )

  private val gatheringFormUi = GatheringFormUi(helpers)

  def edit(swiss: Swiss, form: Form[SwissForm.SwissData])(using Context) =
    Page(swiss.name)
      .css("swiss.form")
      .js(Esm("bits.tourForm")):
        val fields = new SwissFields(form, swiss.some)
        main(cls := "page-small")(
          div(cls := "swiss__form box box-pad")(
            h1(cls := "box__top")("Edit ", swiss.name),
            postForm(cls := "form3", action := routes.Swiss.update(swiss.id))(
              fields.tournamentFields,
              fields.gameFields,
              fields.featuresFields,
              fields.conditionFields,
              fields.pairingsFields,
              form3.globalError(form),
              form3.actions(
                a(href := routes.Swiss.show(swiss.id))(trans.site.cancel()),
                form3.submit(trans.site.save(), icon = Icon.Trophy.some)
              )
            ),
            postForm(cls := "terminate", action := routes.Swiss.terminate(swiss.id))(
              submitButton(dataIcon := Icon.CautionCircle, cls := "text button button-red yes-no-confirm")(
                trans.site.cancelTournament()
              )
            )
          )
        )

  private final class SwissFields(form: Form[SwissForm.SwissData], swiss: Option[Swiss])(using Context):

    private def disabledAfterStart = swiss.exists(!_.isCreated)

    def tournamentFields =
      form3.fieldset("Tournament", toggle = true.some)(
        form3.split(name, nbRounds),
        form3.split(startsAt, description)
      )

    def gameFields =
      form3.fieldset("Games", toggle = true.some)(
        clock,
        form3.split(variant, position)
      )

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
    def variant =
      form3.group(form("variant"), trans.site.variant(), half = true)(
        form3.select(
          _,
          translatedVariantChoicesWithVariants(_.key.value).map(x => x._1 -> x._2),
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
      )(form3.flatpickr(_)(swiss.exists(!_.isCreated).option(disabled)))

    def conditionFields =
      form3.fieldset("Entry conditions", toggle = swiss.exists(!_.settings.conditions.isDefault).some)(
        form3.split(
          gatheringFormUi.nbRatedGame(form("conditions.nbRatedGame.nb")),
          gatheringFormUi.accountAge(form("conditions.accountAge"))
        ),
        form3.split(
          gatheringFormUi.minRating(form("conditions.minRating.rating")),
          gatheringFormUi.maxRating(form("conditions.maxRating.rating"))
        ),
        form3.split(
          playYourGames,
          (summon[Context].me.exists(_.hasTitle) || Granter.opt(_.ManageTournament)).option:
            gatheringFormUi.titled(form("conditions.titled"))
        ),
        form3.split(
          form3.group(
            form("password"),
            trans.site.tournamentEntryCode(),
            help = trans.site.makePrivateTournament().some,
            half = true
          )(form3.input(_)(autocomplete := "off")),
          allowList
        )
      )

    def featuresFields =
      form3.fieldset("Features", toggle = false.some)(
        form3.split(
          form3.group(form("chatFor"), trans.site.tournChat(), half = true) { f =>
            form3.select(
              f,
              Seq(
                Swiss.ChatFor.NONE -> trans.site.noChat.txt(),
                Swiss.ChatFor.LEADERS -> trans.site.onlyTeamLeaders.txt(),
                Swiss.ChatFor.MEMBERS -> trans.site.onlyTeamMembers.txt(),
                Swiss.ChatFor.ALL -> trans.study.everyone.txt()
              )
            )
          },
          form3.group(form("roundInterval"), trans.swiss.roundInterval(), half = true)(
            form3.select(_, SwissForm.roundIntervalChoices)
          )
        ),
        form3.split(
          form3.nativeCheckboxField(
            form("rated"),
            trans.site.rated(),
            help = trans.site.ratedFormHelp().some,
            half = true
          ),
          form3.hidden(form("rated"), "false".some) // hack allow disabling rated
        )
      )

    def pairingsFields =
      form3.fieldset("Custom pairings", toggle = false.some)(
        form3.split(forbiddenPairings, manualPairings)
      )

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
      form3.nativeCheckboxField(
        form("conditions.playYourGames"),
        trans.swiss.mustHavePlayedTheirLastSwissGame(),
        help = trans.swiss.mustHavePlayedTheirLastSwissGameHelp().some,
        half = true
      ),
      form3.hiddenFalse(form("conditions.playYourGames"))
    )
