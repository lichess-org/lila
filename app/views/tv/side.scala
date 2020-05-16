package views.html.tv

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._

import controllers.routes

object side {

  def channels(
    channel: Option[lidraughts.tv.Tv.Channel],
    champions: lidraughts.tv.Tv.Champions,
    baseUrl: String,
    customTitle: Option[String] = None
  )(implicit ctx: Context): Frag = {
    val isGamesList = baseUrl == "/games"
    def collectionTitle: String = customTitle.getOrElse(" - ")
    frag(
      div(cls := "tv-channels subnav")(
        lidraughts.tv.Tv.Channel.visible.map { c =>
          a(href := s"$baseUrl/${c.key}", cls := List(
            "tv-channel" -> true,
            c.key -> true,
            "active" -> channel.contains(c)
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
        },
        isGamesList option frag(
          a(href := s"$baseUrl/collection", cls := List(
            "tv-channel" -> true,
            "collection" -> true,
            "active" -> channel.isEmpty
          ))(
            span(dataIcon := ".")(
              span(
                strong(trans.collection()),
                span(cls := "champion collection-title")(collectionTitle)
              )
            )
          )
        )
      ),
      (isGamesList && channel.isEmpty) option collectionPanel
    )
  }

  private def collectionPanel(implicit ctx: Context) =
    div(cls := "game__collection")(
      st.section(
        div(cls := "game__collection__buttons")(
          div(
            div(
              /* id with "user" in it makes password managers hog on to the field */
              input(`type` := "text", cls := "user-autocomplete", id := "collection-recent", placeholder := trans.gameByUsername.txt(), dataTag := "span")
            ),
            button(`type` := "button", id := "submit-username", cls := "submit button", title := trans.addOngoingOrRecentGame.txt(), dataIcon := "O")
          ),
          div(
            input(`type` := "text", id := "collection-gameid", placeholder := trans.gameByUrlOrId.txt()),
            button(`type` := "button", id := "submit-gameid", cls := "submit button", title := trans.addGameByUrlOrId.txt(), dataIcon := "O")
          )
        ),
        div(cls := "game__collection__links")(
          a(id := "links-copy", dataIcon := "\"")(trans.copyCollectionUrl()),
          a(id := "links-edit", dataIcon := "%")(trans.editCollection()),
          a(id := "links-next", dataIcon := "P", title := trans.reloadBoardsExplanation.txt())(trans.reloadBoards())
        )
      )
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
            div(cls := s"player text empty")(nbsp),
            div(cls := s"player text")(trans.noGameFound())
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
