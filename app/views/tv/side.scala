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
    baseUrl: String,
    povOption: Option[lila.game.Pov]
  )(implicit ctx: Context): Option[Frag] = ctx.noBlind option div(cls := "side")(
    povOption.map { pov =>
      frag(
        div(cls := "side_box padded")(
          h2(dataIcon := "1", cls := "text")("Lichess TV"),
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
        ),
        pov.game.userIds.filter(isStreaming).map { id =>
          a(href := routes.Streamer.show(id), cls := "context-streamer text side_box", dataIcon := "")(
            usernameOrId(id),
            " is streaming"
          )
        }
      )
    },
    div(cls := "tv-channels subnav")(
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
  )

  def sides(
    channel: lila.tv.Tv.Channel,
    champions: lila.tv.Tv.Champions,
    pov: lila.game.Pov,
    cross: Option[lila.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) =
    div(cls := "sides")(
      side(channel, champions, "/tv", pov.some),
      cross.map { c =>
        views.html.game.crosstable(c, pov.gameId.some)
      }
    )
}
