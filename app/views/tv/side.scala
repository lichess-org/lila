package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object side {

  private val titleTag = h2(dataIcon := "1", cls := "text")

  def apply(
    channel: lidraughts.tv.Tv.Channel,
    champions: lidraughts.tv.Tv.Champions,
    baseUrl: String,
    povOption: Option[lidraughts.game.Pov]
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option div(cls := "side")(
    div(cls := "side_box padded")(
      povOption.fold[Frag](
        if (baseUrl != "/tv") titleTag("Lidraughts games")
        else frag(
          titleTag("Lidraughts TV"),
          br,
          div(cls := "confrontation")(trans.noGameFound()),
          br,
          channel.name
        )
      ) { pov =>
          frag(
            titleTag("Lidraughts TV"),
            br,
            div(cls := "confrontation")(
              playerLink(pov.game.whitePlayer, withRating = false, withOnline = false, withDiff = false),
              em(" vs "),
              playerLink(pov.game.blackPlayer, withRating = false, withOnline = false, withDiff = false)
            ),
            br,
            shortClockName(pov.game.clock.map(_.config)),
            " ",
            views.html.game.bits.variantLink(pov.game.variant, variantName(pov.game.variant)),
            pov.game.rated option frag(", ", trans.rated())
          )
        }
    ),
    povOption.map { pov =>
      pov.game.userIds.filter(isStreaming).map { id =>
        a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
          usernameOrId(id),
          " is streaming"
        )
      }
    },
    div(id := "tv_channels")(
      lidraughts.tv.Tv.Channel.visible.map { c =>
        a(dataIcon := c.icon, href := s"$baseUrl/${c.key}", cls := List(c.key -> true, "active" -> (c == channel)))(
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
      }
    )
  )

  def sides(
    channel: lidraughts.tv.Tv.Channel,
    champions: lidraughts.tv.Tv.Champions,
    pov: lidraughts.game.Pov,
    cross: Option[lidraughts.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) =
    div(cls := "sides")(
      side(channel, champions, "/tv", pov.some),
      cross.map { c =>
        div(cls := "crosstable")(views.html.game.crosstable(c, pov.gameId.some))
      }
    )
}
