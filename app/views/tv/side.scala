package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object side {

  def apply(
    channel: lidraughts.tv.Tv.Channel,
    champions: lidraughts.tv.Tv.Champions,
    baseUrl: String
  )(implicit ctx: Context): Frag = div(cls := "tv-channels subnav")(
    lidraughts.tv.Tv.Channel.all.map { c =>
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
    channel: lidraughts.tv.Tv.Channel,
    champions: lidraughts.tv.Tv.Champions,
    povOption: Option[lidraughts.game.Pov],
    cross: Option[lidraughts.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) =
    div(cls := "sides")(
      side(channel, champions, "/tv"),
      povOption ?? { pov =>
        cross.map { c =>
          views.html.game.crosstable(c, pov.gameId.some)
        }
      }
    )
}
