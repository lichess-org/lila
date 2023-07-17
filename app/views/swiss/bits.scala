package views.html.swiss

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.i18n.{ I18nKeys as trans }
import lila.swiss.Swiss
import play.api.i18n.Lang

import controllers.routes

object bits:

  def link(swiss: Swiss): Frag     = link(swiss.id, swiss.name)
  def link(swissId: SwissId): Frag = link(swissId, idToName(swissId))
  def link(swissId: SwissId, name: String): Frag =
    a(
      dataIcon := licon.Trophy,
      cls      := "text",
      href     := routes.Swiss.show(swissId).url
    )(name)

  def idToName(id: SwissId): String = env.swiss.getName sync id getOrElse "Tournament"

  def notFound()(using PageContext) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(cls := "box__top")(trans.tournamentNotFound()),
        p(trans.tournamentDoesNotExist()),
        p(trans.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Swiss.home)(trans.returnToTournamentsHomepage())
      )
    }

  def forTeam(swisses: List[Swiss])(using PageContext) =
    table(cls := "slist")(
      tbody(
        swisses map { s =>
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
                  if s.settings.rated then trans.ratedTournament() else trans.casualTournament(),
                  " • ",
                  s.estimatedDurationString
                )
              )
            ),
            td(cls := "infos")(
              momentFromNowOnce(s.startsAt)
            ),
            td(cls := "text", dataIcon := licon.User)(s.nbPlayers.localize)
          )
        }
      )
    )

  def showInterval(s: Swiss)(using Lang): Frag =
    s.settings.dailyInterval match
      case Some(d)                         => trans.swiss.oneRoundEveryXDays.pluralSame(d)
      case None if s.settings.manualRounds => trans.swiss.roundsAreStartedManually()
      case None =>
        if s.settings.intervalSeconds < 60 then
          trans.swiss.xSecondsBetweenRounds.pluralSame(s.settings.intervalSeconds)
        else trans.swiss.xMinutesBetweenRounds.pluralSame(s.settings.intervalSeconds / 60)

  def homepageSpotlight(s: Swiss)(using PageContext) =
    a(href := routes.Swiss.show(s.id), cls := "tour-spotlight little")(
      iconTag(s.perfType.icon)(cls := "img icon"),
      span(cls := "content")(
        span(cls := "name")(s.name, " Swiss"),
        span(cls := "more")(
          trans.nbPlayers.plural(s.nbPlayers, s.nbPlayers.localize),
          " • ",
          if s.isStarted then trans.eventInProgress() else momentFromNow(s.startsAt)
        )
      )
    )

  def jsI18n(using PageContext) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
    trans.join,
    trans.withdraw,
    trans.youArePlaying,
    trans.joinTheGame,
    trans.signIn,
    trans.averageElo,
    trans.gamesPlayed,
    trans.whiteWins,
    trans.blackWins,
    trans.draws,
    trans.winRate,
    trans.performance,
    trans.standByX,
    trans.averageOpponent,
    trans.tournamentComplete,
    trans.tournamentEntryCode,
    trans.swiss.viewAllXRounds,
    trans.swiss.ongoingGames,
    trans.swiss.startingIn,
    trans.swiss.nextRound,
    trans.swiss.nbRounds,
    trans.team.joinTeam
  )
