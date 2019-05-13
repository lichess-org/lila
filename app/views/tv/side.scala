package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  def channels(
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
            span(cls := "champion")(
              champions.get(c).fold[Frag](raw(" - ")) { p =>
                frag(
                  p.user.title.fold[Frag](p.user.name)(t => frag(t, nbsp, p.user.name)),
                  " ",
                  p.rating
                )
              }
            )
          )
        )
      )
    }
  )

  private val separator = " • "

  def meta(pov: lidraughts.game.Pov)(implicit ctx: Context): Frag = {
    import pov._
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := views.html.game.bits.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.html.game.widgets showClock game,
              separator,
              (if (game.rated) trans.rated else trans.casual).txt(),
              separator,
              if (game.variant.exotic)
                views.html.game.bits.variantLink(game.variant, game.variant.name.toUpperCase)
              else game.perfType.map { pt =>
                span(title := pt.title)(pt.shortName)
              }
            )
          )
        ),
        div(cls := "game__meta__players")(
          game.players.map { p =>
            div(cls := s"player color-icon is ${p.color.name} text")(
              playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
            )
          }
        )
      ),
      game.tournamentId map { tourId =>
        st.section(cls := "game__tournament-link")(
          a(href := routes.Tournament.show(tourId), dataIcon := "g", cls := "text")(tournamentIdToName(tourId))
        )
      }
    )
  }

  def sides(
    povOption: Option[lidraughts.game.Pov],
    cross: Option[lidraughts.game.Crosstable.WithMatchup]
  )(implicit ctx: Context) =
    div(cls := "sides")(
      povOption ?? { pov =>
        cross.map {
          views.html.game.crosstable(_, pov.gameId.some)
        }
      }
    )
}
