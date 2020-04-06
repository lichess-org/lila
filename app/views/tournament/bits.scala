package views.html.tournament

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }

import controllers.routes

object bits {

  def notFound()(implicit ctx: Context) =
    views.html.base.layout(
      title = trans.tournamentNotFound.txt()
    ) {
      main(cls := "page-small box box-pad")(
        h1(trans.tournamentNotFound()),
        p(trans.tournamentDoesNotExist()),
        p(trans.tournamentMayHaveBeenCanceled()),
        br,
        br,
        a(href := routes.Tournament.home())(trans.returnToTournamentsHomepage())
      )
    }

  def enterable(tours: List[lila.tournament.Tournament]) =
    table(cls := "tournaments")(
      tours map { tour =>
        tr(
          td(cls := "name")(
            a(cls := "text", dataIcon := tournamentIconChar(tour), href := routes.Tournament.show(tour.id))(
              tour.name
            )
          ),
          tour.schedule.fold(td) { s =>
            td(momentFromNow(s.at))
          },
          td(tour.durationString),
          td(dataIcon := "r", cls := "text")(tour.nbPlayers)
        )
      }
    )

  def forTeam(tours: List[lila.tournament.Tournament])(implicit ctx: Context) =
    table(cls := "slist")(
      tbody(
        tours map { t =>
          tr(
            td(cls := "icon")(iconTag(tournamentIconChar(t))),
            td(cls := "header")(
              a(href := routes.Tournament.show(t.id))(
                span(cls := "name")(t.name()),
                span(cls := "setup")(
                  t.clock.show,
                  " • ",
                  if (t.variant.exotic) t.variant.name else t.perfType.map(_.trans),
                  !t.position.initial option frag(" • ", trans.thematic()),
                  " • ",
                  t.mode.fold(trans.casualTournament, trans.ratedTournament)(),
                  " • ",
                  t.durationString
                )
              )
            ),
            td(cls := "infos")(
              t.teamBattle map { battle =>
                frag(battle.teams.size, " teams battle")
              } getOrElse {
                "Inner team"
              },
              br,
              momentFromNowOnce(t.startsAt)
            ),
            td(cls := "text", dataIcon := "r")(t.nbPlayers.localize)
          )
        }
      )
    )

  def jsI18n(implicit ctx: Context) = i18nJsObject(i18nKeys)

  private val i18nKeys = List(
    trans.standing,
    trans.starting,
    trans.tournamentIsStarting,
    trans.youArePlaying,
    trans.standByX,
    trans.tournamentPairingsAreNowClosed,
    trans.join,
    trans.withdraw,
    trans.joinTheGame,
    trans.signIn,
    trans.averageElo,
    trans.gamesPlayed,
    trans.nbPlayers,
    trans.winRate,
    trans.berserkRate,
    trans.performance,
    trans.tournamentComplete,
    trans.movesPlayed,
    trans.whiteWins,
    trans.blackWins,
    trans.draws,
    trans.nextXTournament,
    trans.viewMoreTournaments,
    trans.averageOpponent,
    trans.ratedTournament,
    trans.casualTournament
  ).map(_.key)
}
