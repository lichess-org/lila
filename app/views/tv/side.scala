package views.html.tv

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object side {

  def apply(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    baseUrl: String
  )(implicit ctx: Context): Frag = div(cls := "tv-channels subnav")(
    lila.tv.Tv.Channel.all.map { c =>
      a(href := s"$baseUrl/${c.key}", cls := List(
        "tv-channel" -> true,
        c.key -> true,
        "active" -> (c == channel)
      ))(
        span(dataIcon := c.icon)(
          span(
            strong(c.name),
            span(
              champions.get(c).fold[Frag](raw(" - ")) { p =>
                frag(
                  p.user.title.fold[Frag](p.user.name)(t => frag(t, nbsp, p.user.name)),
                  nbsp,
                  p.rating
                )
              }
            )
          )
        )
      )
    }
  )

  def sides(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    pov: lila.game.Pov,
    cross: Option[lila.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) =
    div(cls := "sides")(
      side(channel, champions, "/tv"),
      cross.map { c =>
        views.html.game.crosstable(c, pov.gameId.some)
      }
    )
}
