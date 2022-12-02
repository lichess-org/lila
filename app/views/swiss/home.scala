package views.html.swiss

import controllers.routes
import play.api.i18n.Lang

import lila.api.{ Context, given }
import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.swiss.{ FeaturedSwisses, Swiss }

object home:

  def apply(featured: FeaturedSwisses)(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.swiss.swissTournaments.txt(),
      moreCss = cssTag("swiss.home"),
      withHrefLangs = lila.common.LangPath(routes.Swiss.home).some
    ) {
      main(cls := "page-small box box-pad page swiss-home")(
        h1(cls := "box__top")("Swiss tournaments"),
        renderList(trans.swiss.nowPlaying.txt())(featured.started),
        renderList(trans.swiss.startingSoon.txt())(featured.created),
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
              a(href := routes.Swiss.show(s.id))(
                span(cls := "name")(s.name),
                trans.by(span(cls := "team")(teamIdToName(s.teamId)))
              )
            ),
            td(cls := "infos")(
              span(cls := "rounds")(
                if (s.isStarted)
                  trans.swiss.xOutOfYRoundsSwiss
                    .plural(s.settings.nbRounds, s.round.value, s.settings.nbRounds)
                else
                  trans.swiss.xRoundsSwiss.pluralSame(s.settings.nbRounds)
              ),
              span(cls := "setup")(
                s.clock.show,
                " • ",
                if (s.variant.exotic) s.variant.name else s.perfType.trans,
                " • ",
                (if (s.settings.rated) trans.ratedTournament else trans.casualTournament) ()
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

  private def faqEntry(title: Frag, content: Frag)(using Lang) =
    div(cls := "faq")(
      i("?"),
      p(strong(title), content)
    )

  private def faq(using Lang) = frag(
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
      frag("Early draws"),
      frag(
        "In swiss games, players cannot draw before 30 moves are played. While this measure cannot prevent pre-arranged draws, it at least makes it harder to agree to a draw on the fly."
      )
    ),
    faqEntry(
      trans.swiss.whatIfOneDoesntPlayQ(),
      trans.swiss.whatIfOneDoesntPlayA()
    ),
    faqEntry(
      "Protection against no-show",
      "Players who sign up for swiss events but don't play their games can be problematic. To alleviate this issue, Lichess prevents players who failed to play a game from joining a new Swiss event for a given amount of time. The creator of a Swiss event can decide to let them join the event anyway."
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
