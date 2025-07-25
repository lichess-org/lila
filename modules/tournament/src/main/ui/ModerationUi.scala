package lila.tournament
package ui

import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class ModerationUi(helpers: Helpers, ui: TournamentUi):
  import helpers.{ *, given }
  import TournamentModeration.*

  def page(tour: Tournament, view: View, players: List[Player.WithUser])(using Context) =
    Page(view.name).css("tournament.mod"):
      main(cls := "page-menu")(
        lila.ui.bits.pageMenuSubnav(
          View.values.map: v =>
            a(
              cls := v.toString.active(view.toString),
              href := routes.Tournament.moderation(tour.id, v.toString)
            )(v.name)
        ),
        div(cls := "page-menu__content box box-pad")(
          div(cls := "box__top")(
            h1(
              ui.tournamentLink(tour),
              " • ",
              view.name
            )
          ),
          table(cls := "slist tournament__moderation")(
            thead(
              tr(
                th("User"),
                th("Games"),
                th("Created"),
                th("Rating"),
                th("Points"),
                th("Performance")
              )
            ),
            tbody(
              players.map: p =>
                import p.*
                tr(
                  td(userLink(user)),
                  td(user.count.game.localize),
                  td(momentFromNowServer(user.createdAt)),
                  td(player.showRating),
                  td(player.score),
                  td(player.performance)
                )
            )
          )
        )
      )
