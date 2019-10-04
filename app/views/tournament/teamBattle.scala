package views.html
package tournament

import play.api.data.{ Field, Form }

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament
import lila.user.User

import controllers.routes

object teamBattle {

  def edit(tour: Tournament, form: Form[_])(implicit ctx: Context) = views.html.base.layout(
    title = tour.fullName,
    moreCss = cssTag("tournament.form"),
    moreJs = jsTag("tournamentTeamBattleForm.js")
  )(main(cls := "page-small")(
      div(cls := "tour__form box box-pad")(
        h1(tour.fullName),
        if (tour.isFinished) p("This tournament is over, and the teams can no longer be updated.")
        else p("List the teams that will compete in this battle."),
        postForm(cls := "form3", action := routes.Tournament.teamBattleUpdate(tour.id))(
          form3.group(form("teams"), raw("One team per line. Use the auto-completion."),
            help = frag("You can copy-paste this list from a tournament to another!", br,
              "You can't remove a team if a player has already joined the tournament with it").some)(
              form3.textarea(_)(rows := 10, tour.isFinished.option(disabled))
            ),
          form3.group(form("nbTopPlayers"), raw("Number of leaders per team. The sum of their score is the score of the team."),
            help = frag("You really shouldn't change this value after the tournament has started!").some)(
            form3.input(_)(tpe := "number")
          ),
          form3.globalError(form),
          form3.submit("Update teams")(tour.isFinished.option(disabled))
        )
      )
    ))

  def list(tours: List[Tournament])(implicit ctx: Context) =
    table(cls := "slist")(
      tbody(
        tours map { t =>
          tr(
            td(cls := "icon")(iconTag(tournamentIconChar(t))),
            td(cls := "header")(
              a(href := routes.Tournament.show(t.id))(
                span(cls := "name")(t.fullName),
                span(cls := "setup")(
                  t.clock.show,
                  " • ",
                  if (t.variant.exotic) t.variant.name else t.perfType.map(_.name),
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
}
