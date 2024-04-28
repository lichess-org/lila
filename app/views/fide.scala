package views.fide

import scalalib.paginator.Paginator

import lila.app.templating.Environment.{ *, given }
import lila.core.fide.FideTC
import lila.fide.{ Federation, FidePlayer }
import lila.relay.RelayTour

private lazy val ui = lila.fide.ui.FideUi(helpers)

private def layout(title: String, active: String)(modifiers: Modifier*)(using PageContext) =
  views.base.layout(
    moreCss = cssTag("fide"),
    title = title,
    modules = infiniteScrollEsmInit
  ):
    main(cls := "page-menu")(
      views.relay.tour.pageMenu(active),
      div(cls := "page-menu__content box")(modifiers)
    )

object federation:

  def index(feds: Paginator[Federation])(using PageContext) =
    layout("FIDE federations", "federations")(
      cls := "fide-federations",
      ui.federation.index(feds)
    )

  def show(fed: Federation, players: Paginator[FidePlayer])(using PageContext) =
    layout(s"${fed.name} - FIDE federation", "federations")(
      cls := "fide-federation",
      ui.federation.show(fed, players)
    )

object player:

  def index(players: Paginator[FidePlayer], query: String)(using PageContext) =
    layout("FIDE players", "players")(
      cls := "fide-players",
      ui.player.index(players, query)
    )

  def show(player: FidePlayer, tours: Paginator[RelayTour.WithLastRound])(using PageContext) =
    layout(s"${player.name} - FIDE player ${player.id}", "players")(
      cls := "box-pad fide-player",
      ui.player.show(
        player,
        (tours.nbResults > 0).option(
          views.relay.tour.renderPager(views.relay.tour.asRelayPager(tours)): page =>
            routes.Fide.show(player.id, player.slug, page)
        )
      )
    )
