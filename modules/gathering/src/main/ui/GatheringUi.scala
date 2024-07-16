package lila.gathering
package ui

import lila.core.data.UserIds
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class GatheringUi(helpers: Helpers)(prizeTournamentMakers: () => UserIds):
  import helpers.{ *, given }

  def userPrizeDisclaimer(ownerId: UserId): Option[Frag] =
    (!prizeTournamentMakers().value.contains(ownerId)).option:
      div(cls := "tour__prize")(
        "This tournament is not organized by Lichess.",
        br,
        "If it has prizes, Lichess is not responsible for paying them."
      )

  def verdicts(vs: Condition.WithVerdicts, pk: PerfKey, relevant: Boolean = true)(using
      ctx: Context
  ): Option[Tag] =
    vs.nonEmpty.option(
      st.section(
        dataIcon := relevant.option(if ctx.isAuth && vs.accepted then Icon.Checkmark else Icon.Padlock),
        cls := List(
          "conditions" -> true,
          "accepted"   -> (relevant && ctx.isAuth && vs.accepted),
          "refused"    -> (relevant && ctx.isAuth && !vs.accepted)
        )
      )(
        div(
          (vs.list.sizeIs < 2).option(p(trans.site.conditionOfEntry())),
          vs.list.map: v =>
            p(
              cls := List(
                "condition" -> true,
                "accepted"  -> (relevant && ctx.isAuth && v.verdict.accepted),
                "refused"   -> (relevant && ctx.isAuth && !v.verdict.accepted)
              ),
              title := v.verdict.reason.map(_(ctx.translate))
            ):
              v.condition match
                case Condition.TeamMember(teamId, teamName) =>
                  trans.site.mustBeInTeam(teamLink(teamId, withIcon = false))
                case condition =>
                  v.verdict match
                    case Condition.RefusedUntil(until) =>
                      frag(
                        "Because you missed your last Swiss game, you cannot enter a new Swiss tournament until ",
                        absClientInstant(until),
                        "."
                      )
                    case _ => condition.name(pk)
        )
      )
    )

final class GatheringFormUi(helpers: Helpers):
  import helpers.*
  import play.api.data.Field

  val titleBypass = frag("Titled players bypass this restriction.")

  def nbRatedGame(field: Field)(using Translate) =
    form3.group(
      field,
      trans.site.minimumRatedGames(),
      help = titleBypass.some,
      half = true
    ):
      form3.select(_, ConditionForm.nbRatedGameChoices)

  def minRating(field: Field)(using Translate) =
    form3.group(field, trans.site.minimumRating(), half = true):
      form3.select(_, ConditionForm.minRatingChoices)

  def maxRating(field: Field)(using Translate) =
    form3.group(field, trans.site.maximumWeeklyRating(), half = true):
      form3.select(_, ConditionForm.maxRatingChoices)

  def accountAge(field: Field)(using Translate) =
    form3.group(
      field,
      "Minimum account age",
      help = titleBypass.some,
      half = true
    ):
      form3.select(_, ConditionForm.accountAgeChoices)

  def allowList(field: Field)(using Translate) =
    form3.group(
      field,
      trans.swiss.predefinedUsers(),
      help = trans.swiss.forbiddedUsers().some,
      half = true
    )(form3.textarea(_)(rows := 4))

  def titled(field: Field)(using Translate) =
    form3.checkbox(
      field,
      trans.arena.onlyTitled(),
      help = trans.arena.onlyTitledHelp().some,
      half = true
    )
