package lila.swiss
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SwissBitsUi(helpers: Helpers, getName: GetSwissName):
  import helpers.{ *, given }

  def link(swiss: Swiss): Frag     = link(swiss.id, swiss.name)
  def link(swissId: SwissId): Frag = link(swissId, idToName(swissId))
  def link(swissId: SwissId, name: String): Frag =
    a(
      dataIcon := Icon.Trophy,
      cls      := "text",
      href     := routes.Swiss.show(swissId).url
    )(name)

  def idToName(id: SwissId): String = getName.sync(id).getOrElse("Tournament")

  def notFound(using Context) =
    Page(trans.site.tournamentNotFound.txt()):
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.site.tournamentNotFound()),
        p(trans.site.tournamentDoesNotExist()),
        p(trans.site.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Swiss.home)(trans.site.returnToTournamentsHomepage())
      )

  def forTeam(swisses: List[Swiss])(using Context) =
    table(cls := "slist")(
      tbody(
        swisses.map { s =>
          tr(
            cls := List(
              "enterable" -> s.isNotFinished,
              "soon"      -> s.isNowOrSoon
            )
          )(
            td(cls := "icon")(iconTag(s.perfType.icon)),
            td(cls := "header")(
              a(href := routes.Swiss.show(s.id))(
                span(cls := "name")(s.name),
                span(cls := "setup")(
                  s.clock.show,
                  " • ",
                  if s.variant.exotic then s.variant.name else s.perfType.trans,
                  " • ",
                  if s.settings.rated then trans.site.ratedTournament() else trans.site.casualTournament(),
                  " • ",
                  s.estimatedDurationString
                )
              )
            ),
            td(cls := "infos")(
              momentFromNowOnce(s.startsAt)
            ),
            td(cls := "text", dataIcon := Icon.User)(s.nbPlayers.localize)
          )
        }
      )
    )

  def showInterval(s: Swiss)(using Translate): Frag =
    s.settings.dailyInterval match
      case Some(d)                         => trans.swiss.oneRoundEveryXDays.pluralSame(d)
      case None if s.settings.manualRounds => trans.swiss.roundsAreStartedManually()
      case None =>
        if s.settings.intervalSeconds < 60 then
          trans.swiss.xSecondsBetweenRounds.pluralSame(s.settings.intervalSeconds)
        else trans.swiss.xMinutesBetweenRounds.pluralSame(s.settings.intervalSeconds / 60)

  def homepageSpotlight(s: Swiss)(using Context) =
    a(href := routes.Swiss.show(s.id), cls := "tour-spotlight little")(
      iconTag(s.perfType.icon)(cls := "img icon"),
      span(cls := "content")(
        span(cls := "name")(s.name, " Swiss"),
        span(cls := "more")(
          trans.site.nbPlayers.plural(s.nbPlayers, s.nbPlayers.localize),
          " • ",
          if s.isStarted then trans.site.eventInProgress() else momentFromNow(s.startsAt)
        )
      )
    )

  def jsI18n(using Context) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
    trans.site.join,
    trans.site.withdraw,
    trans.site.youArePlaying,
    trans.site.joinTheGame,
    trans.site.signIn,
    trans.site.averageElo,
    trans.site.gamesPlayed,
    trans.site.whiteWins,
    trans.site.blackWins,
    trans.site.drawRate,
    trans.swiss.byes,
    trans.swiss.absences,
    trans.study.downloadAllGames,
    trans.site.winRate,
    trans.site.points,
    trans.swiss.tieBreak,
    trans.site.performance,
    trans.site.standByX,
    trans.site.averageOpponent,
    trans.site.tournamentComplete,
    trans.site.tournamentEntryCode,
    trans.swiss.viewAllXRounds,
    trans.swiss.ongoingGames,
    trans.swiss.startingIn,
    trans.swiss.nextRound,
    trans.swiss.nbRounds,
    trans.team.joinTeam
  )
