package views.fide

import scalalib.paginator.Paginator

import lila.app.UiEnv.*
import lila.fide.{ FidePlayer, FideRatingHistory }
import lila.relay.RelayTour

private def broadcastOrPlayerMenu(helpers: lila.ui.Helpers): String => lila.ui.Context ?=> Frag = active =>
  ctx ?=>
    import helpers.given
    if ctx.req.queryString.contains("community") then views.user.bits.communityMenu("fide")
    else views.relay.menu(active)

lazy val ui = lila.fide.ui.FideUi(helpers)(broadcastOrPlayerMenu(helpers))
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
