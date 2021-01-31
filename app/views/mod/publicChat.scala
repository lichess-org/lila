package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText

import controllers.routes

object publicChat {

  def apply(
      tourChats: List[(lila.tournament.Tournament, lila.chat.UserChat)],
      simulChats: List[(lila.simul.Simul, lila.chat.UserChat)]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = "Public Chats",
      moreCss = cssTag("mod.communication"),
      moreJs = jsTag("public-chat.js")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("public-chat"),
        div(id := "comm-wrap")(
          div(id := "communication", cls := "page-menu__content public_chat box box-pad")(
            h2("Tournament Chats"),
            div(cls := "player_chats")(
              tourChats.map { case (tournament, chat) =>
                div(cls := "game")(
                  a(cls := "title", href := routes.Tournament.show(tournament.id))(tournament.name),
                  div(cls := "chat")(
                    chat.lines.filter(_.isVisible).map { line =>
                      div(cls := "line")(
                        userIdLink(line.author.toLowerCase.some, withOnline = false, withTitle = false),
                        " ",
                        richText(line.text, expandImg = false)
                      )
                    }
                  )
                )
              }
            ),
            div(
              h2("Simul Chats"),
              div(cls := "player_chats")(
                simulChats.map { case (simul, chat) =>
                  div(cls := "game")(
                    a(cls := "title", href := routes.Simul.show(simul.id))(simul.name),
                    div(cls := "chat")(
                      chat.lines.filter(_.isVisible).map { line =>
                        div(cls := "line")(
                          userIdLink(line.author.toLowerCase.some, withOnline = false, withTitle = false),
                          " ",
                          richText(line.text, expandImg = false)
                        )
                      }
                    )
                  )
                }
              )
            )
          )
        )
      )
    }
}
