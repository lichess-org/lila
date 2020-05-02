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
    lidraughts.tv.Tv.Channel.visible.map { c =>
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

  def meta(povOption: Option[lidraughts.game.Pov], channel: lidraughts.tv.Tv.Channel)(implicit ctx: Context): Frag = {
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := povOption.fold(channel.icon)(pov => views.html.game.bits.gameIcon(pov.game).toString))(
          div(cls := "header")(
            div(cls := "setup")(
              povOption.fold(frag(s"${channel.name} TV")) { pov =>
                frag(
                  views.html.game.widgets.showClock(pov.game),
                  separator,
                  (if (pov.game.rated) trans.rated else trans.casual).txt(),
                  separator,
                  if (pov.game.variant.exotic)
                    views.html.game.bits.variantLink(pov.game.variant, pov.game.variant.name.toUpperCase)
                  else pov.game.perfType.map { pt =>
                    span(title := pt.title)(pt.shortName)
                  }
                )
              }
            )
          )
        ),
        div(cls := "game__meta__players")(
          povOption.fold(frag(
            div(cls := s"player text")(trans.noGameFound()),
            div(cls := s"player text")(nbsp)
          )) {
            _.game.players.map { p =>
              div(cls := s"player color-icon is ${p.color.name} text")(
                playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
              )
            }
          }
        )
      ),
      povOption flatMap { _.game.tournamentId } map { tourId =>
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
