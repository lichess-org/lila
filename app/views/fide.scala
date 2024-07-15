package views.fide

import scalalib.paginator.Paginator

import lila.app.UiEnv.*
import lila.fide.FidePlayer
import lila.relay.RelayTour

lazy val ui = lila.fide.ui.FideUi(helpers)(active => Context ?=> views.relay.tour.pageMenu(active))
export ui.federation

object player:
  export ui.player.{ index, notFound }

  def show(player: FidePlayer, tours: Paginator[RelayTour.WithLastRound])(using Context) =
    ui.player.show(
      player,
      (tours.nbResults > 0).option:
        views.relay.tour.renderPager(views.relay.tour.asRelayPager(tours)):
          routes.Fide.show(player.id, player.slug, _)
    )
