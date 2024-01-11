package views.html.team

import play.api.i18n.Lang

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.app.mashup.TeamInfo

import controllers.routes

object tournaments {

  def page(t: lila.team.Team, tours: TeamInfo.PastAndNext)(implicit ctx: Context) = {
    views.html.base.layout(
      title = s"${t.name} - ${trans.tournaments.txt()}",
      moreCss = cssTag("team"),
      wrapClass = "full-screen-force"
    ) {
      main(
        div(cls := "box")(
          h1(
            views.html.team.bits.link(t),
            " - ",
            trans.tournaments()
          ),
          div(cls := "team-tournaments team-tournaments--both")(
            div(cls := "team-tournaments__next")(
              h2(trans.team.upcomingTourns()),
              table(cls := "slist slist-pad slist-invert")(
                renderList(tours.next)
              )
            ),
            div(cls := "team-tournaments__past")(
              h2(trans.team.completedTourns()),
              table(cls := "slist slist-pad")(
                renderList(tours.past)
              )
            )
          )
        )
      )
    }
  }

  def renderList(tours: List[lila.tournament.Tournament])(implicit ctx: Context) =
    tbody(
      tours map { tour =>
        tr(
          cls := List(
            "enterable" -> tour.isEnterable,
            "soon"      -> tour.isNowOrSoon
          )
        )(
          td(cls := "icon")(iconTag(tournamentIconChar(tour))),
          td(cls := "header")(
            a(href := routes.Tournament.show(tour.id))(
              span(cls := "name")(tour.name()),
              span(cls := "setup")(
                tour.clock.show,
                " - ",
                if (!tour.variant.standard) variantName(tour.variant) else tour.perfType.map(_.trans),
                tour.position.isDefined option frag(" - ", trans.thematic()),
                " - ",
                tour.mode.fold(trans.casualTournament, trans.ratedTournament)(),
                " - ",
                tour.durationString
              )
            )
          ),
          td(cls := "infos")(
            frag(
              tour.teamBattle map { battle =>
                frag(battle.teams.size, " teams battle")
              } getOrElse "Inner team",
              br,
              renderStartsAt(tour)
            )
          ),
          td(cls := "text", dataIcon := "r")(tour.nbPlayers.localize)
        )
      }
    )

  private def renderStartsAt(tour: lila.tournament.Tournament)(implicit lang: Lang): Frag =
    if (tour.isEnterable && tour.startsAt.isBeforeNow) trans.playingRightNow()
    else momentFromNowOnce(tour.startsAt)
}
