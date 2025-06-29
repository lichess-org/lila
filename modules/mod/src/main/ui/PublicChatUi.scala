package lila.mod
package ui

import lila.chat.{ ChatTimeout, UserChat }
import lila.ui.*

import ScalatagsTemplate.{ *, given }

final class PublicChatUi(helpers: Helpers, modUi: ModUi)(highlightBad: String => Frag):
  import helpers.{ *, given }

  def apply(
      tourChats: List[(lila.core.tournament.Tournament, UserChat)],
      swissChats: List[(lila.core.swiss.IdName, UserChat)]
  )(using Context) =
    Page("Public Chats")
      .css("mod.publicChats")
      .js(Esm("bits.publicChats")):
        main(cls := "page-menu")(
          bits.modMenu("public-chat"),
          div(id := "comm-wrap")(
            div(id := "communication", cls := "page-menu__content public-chat box box-pad")(
              h2("Tournament Chats"),
              div(cls := "player_chats"):
                tourChats.map: (tournament, chat) =>
                  div(cls := "game", dataChan := "tournament", dataRoom := tournament.id):
                    chatOf(tournamentTitle(tournament), chat)
              ,
              div(
                h2("Swiss Chats"),
                div(cls := "player_chats"):
                  swissChats.map: (swiss, chat) =>
                    div(cls := "game", dataChan := "swiss", dataRoom := swiss.id):
                      chatOf(swissTitle(swiss), chat)
              ),
              div(cls := "timeout-modal none")(
                h2(cls := "username")("username"),
                p(cls := "text")("text"),
                div(cls := "continue-with"):
                  ChatTimeout.Reason.all.map: reason =>
                    button(cls := "button", value := reason.key)(reason.shortName)
              )
            )
          )
        )

  private val dataRoom = attr("data-room")
  private val dataChan = attr("data-chan")

  private def chatOf(titleFragment: Frag, chat: UserChat)(using Context) =
    frag(
      titleFragment,
      div(cls := "chat"):
        chat.lines
          .filter(_.isVisible)
          .map: line =>
            div(
              cls := List(
                "line"    -> true,
                "lichess" -> line.isLichess
              )
            )(
              userIdLink(UserStr(line.author).id.some, withOnline = false, withTitle = false),
              " ",
              highlightBad(line.text)
            )
    )

  private def swissTitle(swiss: lila.core.swiss.IdName) =
    a(cls := "title", href := routes.Swiss.show(swiss.id))(swiss.name)

  private def tournamentTitle(tournament: lila.core.tournament.Tournament) =
    div(cls := "title-time")(
      a(cls := "title", href := routes.Tournament.show(tournament.id))(tournament.name),
      span(cls := s"tournament-status ${tournament.status.name}")(tournament.status.name)
    )
