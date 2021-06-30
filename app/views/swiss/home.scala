package views.html.swiss

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.swiss.{ FeaturedSwisses, Swiss }

import controllers.routes

object home {

  def apply(featured: FeaturedSwisses)(implicit ctx: Context) =
    views.html.base.layout(
      title = "Swiss tournaments",
      moreCss = cssTag("swiss.home")
    ) {
      main(cls := "page-small box box-pad page swiss-home")(
        h1("Swiss tournaments"),
        renderList("Now playing")(featured.started),
        renderList("Starting soon")(featured.created),
        div(cls := "swiss-home__infos")(
          div(cls := "wiki")(
            iconTag(""),
            p(
              trans.swiss.swissDescription(
                a(href := "https://en.wikipedia.org/wiki/Swiss-system_tournament")("(wiki)")
              )
            )
          ),
          div(cls := "team")(
            iconTag(""),
           p(
              trans.swiss.teamOnly(
                a(href := routes.Team.home())(trans.swiss.joinOrCreateTeam.txt())
              )
            )
          ),
          comparison,
          div(id := "faq")(faq)
        )
      )
    }

  private def renderList(name: String)(swisses: List[Swiss])(implicit ctx: Context) =
    table(cls := "slist swisses")(
      thead(tr(th(colspan := 4)(name))),
      tbody(
        swisses map { s =>
          tr(
            td(cls := "icon")(iconTag(bits.iconChar(s))),
            td(cls := "header")(
              a(href := routes.Swiss.show(s.id.value))(
                span(cls := "name")(s.name),
                trans.by(span(cls := "team")(teamIdToName(s.teamId)))
              )
            ),
            td(cls := "infos")(
              span(cls := "rounds")(
                s.isStarted option frag(s.round.value, " / "),
                s.settings.nbRounds,
                " rounds Swiss"
              ),
              span(cls := "setup")(
                s.clock.show,
                " • ",
                if (s.variant.exotic) s.variant.name else s.perfType.trans,
                " • ",
                (if (s.settings.rated) trans.ratedTournament else trans.casualTournament)()
              )
            ),
            td(
              momentFromNow(s.startsAt),
              br,
              span(cls := "players text", dataIcon := "")(s.nbPlayers.localize)
            )
          )
        }
      )
    )

 private def comparison(implicit lang: Lang) = table(cls := "comparison slist")(
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
        td(trans.swiss.pairingWaitTime()),
        td(trans.swiss.pairingWaitTime())
      ),
      tr(
        th(trans.swiss.identicalPairing()),
        td(trans.swiss.possibleButNotConsecutive()),
        td(trans.swiss.identicalForbidden())
      ),
      tr(
        th(trans.swiss.lateJoin()),
        td(trans.yes()),
        td(trans.swiss.lateJoinUntil())
      ),
      tr(
        th(trans.swiss.pause()),
        td(trans.yes()),
        td(trans.swiss.pauseSwiss())
      ),
      tr(
        th(trans.swiss.streaksAndBerserk()),
        td(trans.yes()),
        td(trans.no())
      ),
      tr(
        th(trans.swiss.similarToOTB()),
        td(trans.no()),
        td(trans.yes())
      ),
      tr(
        th(trans.swiss.unlimitedAndFree()),
        td(trans.yes()),
        td(trans.yes())
      )
    )
  )
  private def faq(implicit lang: Lang) = frag(
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.swissVsArenaQ()),
        trans.swiss.swissVsArenaA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.pointsCalculationQ()),
        trans.swiss.pointsCalculationA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.tiebreaksCalculationQ()),
        trans.swiss.tiebreaksCalculationA(
          a(
            href := "https://en.wikipedia.org/wiki/Tie-breaking_in_Swiss-system_tournaments#Sonneborn%E2%80%93Berger_score"
          )(trans.swiss.sonnebornBergerScore.txt())
        )
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.pairingsQ()),
        trans.swiss.pairingsA(
          a(
            href := "https://en.wikipedia.org/wiki/Swiss-system_tournament#Dutch_system"
          )(trans.swiss.dutchSystem.txt()),
          a(href := "https://github.com/BieremaBoyzProgramming/bbpPairings")("bbPairings"),
          a(href := "https://handbook.fide.com/chapter/C0403")(trans.swiss.FIDEHandbook.txt())
        )
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.moreRoundsThanPlayersQ()),
        trans.swiss.moreRoundsThanPlayersA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.restrictedToTeamsQ()),
        trans.swiss.restrictedToTeamsANotForOnline(),
        trans.swiss.restrictedToTeamsABetterMet()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.numberOfByesQ()),
        trans.swiss.numberOfByesA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.whatIfOneDoesntPlayQ()),
        trans.swiss.whatIfOneDoesntPlayA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.lateJoinQ()),
        trans.swiss.lateJoinAYes(),
        trans.swiss.lateJoinASingleBye()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.willSwissReplaceArenasQ()),
        trans.swiss.willSwissReplaceArenasA()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.roundRobinQ()),
        trans.swiss.roundRobinAWontWork(),
        trans.swiss.roundRobinANoBecause(),
        trans.swiss.roundRobinATheClosestPossible()
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong(trans.swiss.otherSystemsQ()),
        trans.swiss.otherSystemsA()
      )
    )
  )
}
