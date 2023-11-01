package views.html.tv

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes

object side:

  def channels(
      channel: lila.tv.Tv.Channel,
      champions: lila.tv.Tv.Champions,
      baseUrl: String
  ): Frag =
    views.html.site.bits.subnav(
      lila.tv.Tv.Channel.list.map: c =>
        a(
          href := s"$baseUrl/${c.key}",
          cls := List(
            "tv-channel" -> true,
            c.key        -> true,
            "active"     -> (c == channel)
          )
        ):
          span(dataIcon := c.icon):
            span(
              strong(c.name),
              span(cls := "champion")(
                champions
                  .get(c)
                  .fold[Frag](raw(" - ")): p =>
                    frag(
                      p.user.title.fold[Frag](p.user.name)(t => frag(t, nbsp, p.user.name)),
                      ratingTag(
                        " ",
                        p.rating
                      )
                    )
              )
            )
    )

  private val separator = " • "

  def meta(pov: lila.game.Pov)(using PageContext): Frag =
    import pov.*
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := views.html.game.bits.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.html.game.widgets showClock game,
              separator,
              (if game.rated then trans.rated else trans.casual).txt(),
              separator,
              views.html.game.bits.variantLink(game.variant, game.perfType, shortName = true)
            )
          )
        ),
        div(cls := "game__meta__players"):
          game.players.mapList: p =>
            div(cls := s"player color-icon is ${p.color.name} text"):
              playerLink(p, withOnline = false, withDiff = true, withBerserk = true)
      ),
      game.tournamentId.map: tourId =>
        st.section(cls := "game__tournament-link"):
          a(href := routes.Tournament.show(tourId), dataIcon := licon.Trophy, cls := "text"):
            tournamentIdToName(tourId)
    )

  def sides(
      pov: lila.game.Pov,
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(using Context) =
    div(cls := "sides"):
      cross.map:
        views.html.game.crosstable(_, pov.gameId.some)
