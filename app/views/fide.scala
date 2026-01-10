package views.fide

import scalalib.paginator.Paginator

import lila.app.UiEnv.*
import lila.fide.{ FidePlayer, FideRatingHistory }
import lila.relay.RelayTour

lazy val ui = lila.fide.ui.FideUi(helpers)(active => Context ?=> views.relay.menu(active))
lazy val playerUi = lila.fide.ui.FidePlayerUi(helpers, ui, picfitUrl)
export ui.federation

object player:
  export playerUi.{ index, notFound }

  def show(
      player: FidePlayer,
      user: Option[User],
      tours: Paginator[RelayTour.WithLastRound],
      ratings: FideRatingHistory,
      isFollowing: Boolean
  )(using Context) =
    playerUi.show(
      player,
      user,
      (tours.nbResults > 0).option:
        views.relay.tour.renderPager(views.relay.tour.asRelayPager(tours)):
          routes.Fide.show(player.id, player.slug, _)
      ,
      ratings,
      isFollowing
    )
