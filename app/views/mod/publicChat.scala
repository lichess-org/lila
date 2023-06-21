package views.html.mod

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

import controllers.routes
import lila.chat.UserChat
import lila.chat.ChatTimeout

object publicChat:

  def apply(
      tourChats: List[(lila.tournament.Tournament, UserChat)],
      swissChats: List[(lila.swiss.Swiss, UserChat)]
  )(using PageContext) =
    views.html.base.layout(
      title = "Public Chats",
      moreCss = cssTag("mod.publicChats"),
      moreJs = jsModule("publicChats")
    ) {
      main(cls := "page-menu")(
        views.html.mod.menu("public-chat"),
        div(id := "comm-wrap")(
          div(id := "communication", cls := "page-menu__content public-chat box box-pad")(
            h2("Tournament Chats"),
            div(cls := "player_chats")(
              tourChats.map { case (tournament, chat) =>
                div(cls := "game", dataChan := "tournament", dataRoom := tournament.id)(
                  chatOf(tournamentTitle(tournament), chat)
                )
              }
            ),
            div(
              h2("Swiss Chats"),
              div(cls := "player_chats")(
                swissChats.map { case (swiss, chat) =>
                  div(cls := "game", dataChan := "swiss", dataRoom := swiss.id)(
                    chatOf(swissTitle(swiss), chat)
                  )
                }
              )
            ),
            div(cls := "timeout-modal none")(
              h2(cls := "username")("username"),
              p(cls := "text")("text"),
              div(cls := "continue-with")(
                ChatTimeout.Reason.all.map { reason =>
                  button(cls := "button", value := reason.key)(reason.shortName)
                }
              )
            )
          )
        )
      )
    }

  private val dataRoom = attr("data-room")
  private val dataChan = attr("data-chan")

  private def chatOf(titleFragment: Frag, chat: UserChat)(using PageContext) =
    frag(
      titleFragment,
      div(cls := "chat")(
        chat.lines.filter(_.isVisible).map { line =>
          div(
            cls := List(
              "line"    -> true,
              "lichess" -> line.isLichess
            )
          )(
            userIdLink(UserStr(line.author).id.some, withOnline = false, withTitle = false),
            " ",
            communication.highlightBad(line.text)
          )
        }
      )
    )

  private def swissTitle(swiss: lila.swiss.Swiss) =
    a(cls := "title", href := routes.Swiss.show(swiss.id))(swiss.name)

  private def tournamentTitle(tournament: lila.tournament.Tournament) =
    div(cls := "title-time")(
      a(cls := "title", href := routes.Tournament.show(tournament.id))(tournament.name),
      span(cls := s"tournament-status ${tournament.status.name.toLowerCase}")(tournament.status.name)
    )
