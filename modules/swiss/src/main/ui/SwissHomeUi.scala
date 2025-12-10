package lila.swiss
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class SwissHomeUi(helpers: Helpers):
  import helpers.{ *, given }

  def page(featured: FeaturedSwisses)(using Context) =
    Page(trans.swiss.swissTournaments.txt())
      .css("swiss.home")
      .hrefLangs(lila.ui.LangPath(routes.Swiss.home)):
        main(cls := "page-small box box-pad page swiss-home")(
          h1(cls := "box__top")(trans.swiss.swissTournaments()),
          renderList(trans.swiss.nowPlaying.txt())(featured.started),
          renderList(trans.swiss.startingSoon.txt())(featured.created.sortBy(_.startsAt)),
          div(cls := "swiss-home__infos")(
            div(cls := "wiki")(
              iconTag(Icon.InfoCircle),
              p:
                trans.swiss.swissDescription:
                  a(href := "https://en.wikipedia.org/wiki/Swiss-system_tournament")("(wiki)")
            ),
            div(cls := "team")(
              iconTag(Icon.Group),
              p:
                trans.swiss.teamOnly:
                  a(href := routes.Team.home())(trans.swiss.joinOrCreateTeam.txt())
            ),
            comparison,
            div(id := "faq")(faq)
          )
        )

  private def renderList(name: String)(swisses: List[Swiss])(using Context) =
    table(cls := "slist swisses")(
      thead(tr(th(colspan := 4)(name))),
      tbody:
        swisses.map: s =>
          val team = teamIdToLight(s.teamId)
          tr(
            td(cls := "icon")(iconTag(s.perfType.icon)),
            td(cls := "header")(
              a(href := routes.Swiss.show(s.id))(
                span(cls := "name")(s.name),
                trans.site.by(span(cls := "team")(team.name, teamFlair(team)))
              )
            ),
            td(cls := "infos")(
              span(cls := "rounds")(
                if s.isStarted then
                  trans.swiss.nbRounds.plural(s.settings.nbRounds, s"${s.round}/${s.settings.nbRounds}")
                else trans.swiss.nbRounds.pluralSame(s.settings.nbRounds)
              ),
              span(cls := "setup")(
                s.clock.show,
                " • ",
                if s.variant.exotic then s.variant.name else s.perfType.trans,
                " • ",
                lila.gathering.ui.translateRated(s.settings.rated)
              )
            ),
            td(
              momentFromNow(s.startsAt),
              br,
              span(cls := "players text", dataIcon := Icon.User)(s.nbPlayers.localize)
            )
          )
    )

  private def comparison(using Translate) = table(cls := "comparison slist")(
    thead(
      tr(
        th(trans.swiss.comparison()),
        th(strong(trans.arena.arenaTournaments())),
        th(strong(trans.swiss.swissTournaments()))
      )
    ),
    tbody(
      tr(
        th(trans.swiss.tournDuration()),
        td(trans.swiss.predefinedDuration()),
        td(trans.swiss.durationUnknown())
      ),
      tr(
        th(trans.swiss.numberOfGames()),
        td(trans.swiss.numberOfGamesAsManyAsPossible()),
        td(trans.swiss.numberOfGamesPreDefined())
      ),
      tr(
        th(trans.swiss.pairingSystem()),
        td(trans.swiss.pairingSystemArena()),
        td(trans.swiss.pairingSystemSwiss())
      ),
      tr(
        th(trans.swiss.pairingWaitTime()),
        td(trans.swiss.pairingWaitTimeArena()),
        td(trans.swiss.pairingWaitTimeSwiss())
      ),
      tr(
        th(trans.swiss.identicalPairing()),
        td(trans.swiss.possibleButNotConsecutive()),
        td(trans.swiss.identicalForbidden())
      ),
      tr(
        th(trans.swiss.lateJoin()),
        td(trans.site.yes()),
        td(trans.swiss.lateJoinUntil())
      ),
      tr(
        th(trans.swiss.pause()),
        td(trans.site.yes()),
        td(trans.swiss.pauseSwiss())
      ),
      tr(
        th(trans.swiss.streaksAndBerserk()),
        td(trans.site.yes()),
        td(trans.site.no())
      ),
      tr(
        th(trans.swiss.similarToOTB()),
        td(trans.site.no()),
        td(trans.site.yes())
      ),
      tr(
        th(trans.swiss.unlimitedAndFree()),
        td(trans.site.yes()),
        td(trans.site.yes())
      )
    )
  )

  private def faqEntry(title: Frag, content: Frag) =
    div(cls := "faq")(
      i("?"),
      p(strong(title), content)
    )

  private def faq(using Translate) = frag(
    faqEntry(
      trans.swiss.swissVsArenaQ(),
      trans.swiss.swissVsArenaA()
    ),
    faqEntry(
      trans.swiss.pointsCalculationQ(),
      trans.swiss.pointsCalculationA()
    ),
    faqEntry(
      trans.swiss.tiebreaksCalculationQ(),
      trans.swiss.tiebreaksCalculationA(
        a(
          href := "https://en.wikipedia.org/wiki/Tie-breaking_in_Swiss-system_tournaments#Sonneborn%E2%80%93Berger_score"
        )(trans.swiss.sonnebornBergerScore.txt())
      )
    ),
    faqEntry(
      trans.swiss.pairingsQ(),
      trans.swiss.pairingsA(
        a(
          href := "https://en.wikipedia.org/wiki/Swiss-system_tournament#Dutch_system"
        )(trans.swiss.dutchSystem.txt()),
        a(href := "https://github.com/BieremaBoyzProgramming/bbpPairings")("bbPairings"),
        a(href := "https://handbook.fide.com/chapter/C0403")(trans.swiss.FIDEHandbook.txt())
      )
    ),
    faqEntry(
      trans.swiss.moreRoundsThanPlayersQ(),
      trans.swiss.moreRoundsThanPlayersA()
    ),
    faqEntry(
      trans.swiss.restrictedToTeamsQ(),
      trans.swiss.restrictedToTeamsA()
    ),
    faqEntry(
      trans.swiss.numberOfByesQ(),
      trans.swiss.numberOfByesA()
    ),
    faqEntry(
      trans.swiss.earlyDrawsQ(),
      trans.swiss.earlyDrawsAnswer()
    ),
    faqEntry(
      trans.swiss.whatIfOneDoesntPlayQ(),
      trans.swiss.whatIfOneDoesntPlayA()
    ),
    faqEntry(
      trans.swiss.protectionAgainstNoShowQ(),
      trans.swiss.protectionAgainstNoShowA()
    ),
    faqEntry(
      trans.swiss.lateJoinQ(),
      trans.swiss.lateJoinA()
    ),
    faqEntry(
      trans.swiss.willSwissReplaceArenasQ(),
      trans.swiss.willSwissReplaceArenasA()
    ),
    faqEntry(
      trans.swiss.roundRobinQ(),
      trans.swiss.roundRobinA()
    ),
    faqEntry(
      trans.swiss.otherSystemsQ(),
      trans.swiss.otherSystemsA()
    )
  )
