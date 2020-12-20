package views.html
package tournament

import play.api.data.Form

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.tournament.Tournament

import controllers.routes

object teamBattle {

  def edit(tour: Tournament, form: Form[_])(implicit ctx: Context) =
    views.html.base.layout(
      title = tour.name(),
      moreCss = cssTag("tournament.form"),
      moreJs = frag(
        jsAt("vendor/textcomplete.min.js"),
        jsModule("team-battle-form")
      )
    )(
      main(cls := "page-small")(
        div(cls := "tour__form box box-pad")(
          h1(tour.name()),
          standardFlash(),
          if (tour.isFinished) p("This tournament is over, and the teams can no longer be updated.")
          else p("List the teams that will compete in this battle."),
          postForm(cls := "form3", action := routes.Tournament.teamBattleUpdate(tour.id))(
            form3.group(
              form("teams"),
              raw("One team per line. Use the auto-completion."),
              help = frag(
                "You can copy-paste this list from a tournament to another!",
                br,
                "You can't remove a team if a player has already joined the tournament with it"
              ).some
            )(
              form3.textarea(_)(rows := 10, tour.isFinished.option(disabled))
            ),
            form3.group(
              form("nbLeaders"),
              raw("Number of leaders per team. The sum of their score is the score of the team."),
              help = frag("You really shouldn't change this value after the tournament has started!").some
            )(
              form3.input(_)(tpe := "number")
            ),
            form3.globalError(form),
            form3.submit("Update teams")(tour.isFinished.option(disabled))
          )
        )
      )
    )
}
