package lila.swiss
package ui
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SwissBitsUi(helpers: Helpers, getName: GetSwissName):
  import helpers.{ *, given }

  def link(swiss: Swiss): Tag = link(swiss.id, swiss.name)
  def link(swissId: SwissId): Tag = link(swissId, idToName(swissId))
  def link(swissId: SwissId, name: String): Tag =
    a(
      dataIcon := Icon.Trophy,
      cls := "text",
      href := routes.Swiss.show(swissId).url
    )(name)

  def idToName(id: SwissId): String = getName.sync(id).getOrElse(s"Swiss #$id")

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

  def showInterval(s: Swiss)(using Translate): Frag =
    s.settings.dailyInterval match
      case Some(d) => trans.swiss.oneRoundEveryXDays.pluralSame(d)
      case None if s.settings.manualRounds => trans.swiss.roundsAreStartedManually()
      case None =>
        if s.settings.intervalSeconds < 60 then
          trans.swiss.xSecondsBetweenRounds.pluralSame(s.settings.intervalSeconds)
        else trans.swiss.xMinutesBetweenRounds.pluralSame(s.settings.intervalSeconds / 60)

  def homepageSpotlight(s: Swiss)(using Translate) =
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

  def teamSwissRow(s: Swiss)(using Translate) =
    tr(
      cls := List(
        "enterable" -> s.isNotFinished,
        "soon" -> s.isNowOrSoon
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
            lila.gathering.ui.translateRated(s.settings.rated),
            " • ",
            s.estimatedDurationString
          )
        )
      ),
      td(cls := "infos")(
        trans.swiss.xRoundsSwiss.plural(s.settings.nbRounds, s.settings.nbRounds.localize),
        br,
        if s.isStarted then trans.site.eventInProgress() else momentFromNow(s.startsAt)
      ),
      td(cls := "text", dataIcon := Icon.User)(s.nbPlayers.localize)
    )
