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
              "In a Swiss tournament ",
              a(href := "https://en.wikipedia.org/wiki/Swiss-system_tournament")("(wiki)"),
              ", each competitor does not necessarily play all other entrants. Competitors meet one-on-one in each round and are paired using a set of rules designed to ensure that each competitor plays opponents with a similar running score, but not the same opponent more than once. The winner is the competitor with the highest aggregate points earned in all rounds. All competitors play in each round unless there is an odd number of players."
            )
          ),
          div(cls := "team")(
            iconTag(""),
            p(
              "Swiss tournaments can only be created by team leaders, and can only be played by team members.",
              br,
              a(href := routes.Team.home())("Join or create a team"),
              " to start playing in swiss tournaments."
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

  private lazy val comparison = table(cls := "comparison slist")(
    thead(
      tr(
        th("Comparison"),
        th(strong("Arena"), " tournaments"),
        th(strong("Swiss"), " tournaments")
      )
    ),
    tbody(
      tr(
        th("Duration of the tournament"),
        td("Predefined duration in minutes"),
        td("Predefined max rounds, but duration unknown")
      ),
      tr(
        th("Number of games"),
        td("As many as can be played in the allotted duration"),
        td("Decided in advance, same for all players")
      ),
      tr(
        th("Pairing system"),
        td("Any available opponent with similar ranking"),
        td("Best pairing based on points and tie breaks")
      ),
      tr(
        th("Pairing wait time"),
        td("Fast: doesn't wait for all players"),
        td("Slow: waits for all players")
      ),
      tr(
        th("Identical pairing"),
        td("Possible, but not consecutive"),
        td("Forbidden")
      ),
      tr(
        th("Late join"),
        td("Yes"),
        td("Yes until more than half the rounds have started")
      ),
      tr(
        th("Pause"),
        td("Yes"),
        td("Yes but might reduce the number of rounds")
      ),
      tr(
        th("Streaks & Berserk"),
        td("Yes"),
        td("No")
      ),
      tr(
        th("Similar to OTB tournaments"),
        td("No"),
        td("Yes")
      ),
      tr(
        th("Unlimited and free"),
        td("Yes"),
        td("Yes")
      )
    )
  )

  private lazy val faq = frag(
    div(cls := "faq")(
      i("?"),
      p(
        strong("When to use swiss tournaments instead of arenas?"),
        "In a swiss tournament, all participants play the same number of games, and can only play each other once.",
        br,
        "It can be a good option for clubs and official tournaments."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("How are points calculated?"),
        "A win is worth one point, a draw is a half point, and a loss is zero points.",
        br,
        "When a player can't be paired during a round, they receive a bye worth one point."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("How are tie breaks calculated?"),
        "With the ",
        a(
          href := "https://en.wikipedia.org/wiki/Tie-breaking_in_Swiss-system_tournaments#Sonneborn%E2%80%93Berger_score"
        )("Sonneborn–Berger score"),
        ".",
        br,
        "Add the scores of every opponent the player beats and half of the score of every opponent the player draws."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("How are pairings decided?"),
        "With the ",
        a(
          href := "https://en.wikipedia.org/wiki/Swiss-system_tournament#Dutch_system"
        )("Dutch system"),
        ", implemented by ",
        a(href := "https://github.com/BieremaBoyzProgramming/bbpPairings")("bbPairings"),
        " in accordance with the ",
        a(href := "https://handbook.fide.com/chapter/C0403")("FIDE handbook"),
        "."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("What happens if the tournament has more rounds than players?"),
        "When all possible pairings have been played, the tournament will be ended and a winner declared."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("Why is it restricted to teams?"),
        "Swiss tournaments were not designed for online chess. They demand punctuality, dedication and patience from players.",
        br,
        "We think these conditions are more likely to be met within a team than in global tournaments."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("How many byes can a player get?"),
        "A player gets a bye of one point every time the pairing system can't find a pairing for them.",
        br,
        "Additionally, a single bye of half a point is attributed when a player late-joins a tournament."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("What happens if a player doesn't play a game?"),
        "Their clock will tick, they will flag, and lose the game.",
        br,
        "Then the system will withdraw the player from the tournament, so they don't lose more games.",
        br,
        "They can re-join the tournament at any time."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("Can players late-join?"),
        "Yes, until more than half the rounds have started; for example in a 11-rounds swiss players can join before round 6 starts and in a 12-rounds before round 7 starts.",
        br,
        "Late joiners get a single bye, even if they missed several rounds."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("Will swiss replace arena tournaments?"),
        "No. They're complementary features."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("What about Round Robin?"),
        "We'd like to add it, but unfortunately Round Robin doesn't work online.",
        br,
        "The reason is that it has no fair way of dealing with people leaving the tournament early. ",
        "We cannot expect that all players will play all their games in an online event. ",
        "It just won't happen, and as a result most Round Robin tournaments would be flawed and unfair, ",
        "which defeats their very reason to exist.",
        br,
        "The closest you can get to Round Robin online is to play a Swiss tournament with a very high ",
        "number of rounds. Then all possible pairings will be played before the tournament ends."
      )
    ),
    div(cls := "faq")(
      i("?"),
      p(
        strong("What about other tournament systems?"),
        "We don't plan to add more tournament systems to Lichess at the moment."
      )
    )
  )
}
