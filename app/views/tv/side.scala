package views.tv

import lila.app.templating.Environment.{ *, given }

import lila.game.GameExt.perfType

object side:

  def channels(
      channel: lila.tv.Tv.Channel,
      champions: lila.tv.Tv.Champions,
      baseUrl: String
  ): Frag =
    lila.ui.bits.subnav(
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

  def meta(pov: Pov)(using Context): Frag =
    import pov.*
    div(cls := "game__meta")(
      st.section(
        div(cls := "game__meta__infos", dataIcon := views.game.ui.gameIcon(game))(
          div(cls := "header")(
            div(cls := "setup")(
              views.game.widgets.showClock(game),
              separator,
              (if game.rated then trans.site.rated else trans.site.casual).txt(),
              separator,
              variantLink(game.variant, game.perfType, shortName = true)
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
          a(href := routes.Tournament.show(tourId), dataIcon := Icon.Trophy, cls := "text"):
            views.tournament.ui.tournamentIdToName(tourId)
    )

  def sides(
      pov: Pov,
      cross: Option[lila.game.Crosstable.WithMatchup]
  )(using Context) =
    div(cls := "sides"):
      cross.map:
        views.game.ui.crosstable(_, pov.gameId.some)
