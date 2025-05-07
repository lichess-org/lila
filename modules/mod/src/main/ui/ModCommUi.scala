package lila.mod
package ui

import lila.chat.{ ChatTimeout, UserChat }
import lila.ui.*

import ScalatagsTemplate.{ *, given }
import lila.chat.MixedChat
import lila.core.shutup.{ PublicLine, PublicSource }

final class ModCommUi(helpers: Helpers)(highlightBad: String => Frag):

  import helpers.{ *, given }

  def commsHeader(u: User, priv: Boolean)(using Context, Me) =
    h1(
      div(cls := "title")(userLink(u, params = "?mod"), " communications"),
      div(cls := "actions")(
        a(
          cls  := "button button-empty mod-zone-toggle",
          href := routes.User.mod(u.username),
          titleOrText("Mod zone (Hotkey: m)"),
          dataIcon := Icon.Agent
        ),
        Granter(_.ViewPrivateComms).option:
          if priv then
            a(cls := "priv button active", href := routes.Mod.communicationPublic(u.username))("PMs")
          else
            a(
              cls   := "priv button",
              href  := routes.Mod.communicationPrivate(u.username),
              title := "View private messages. This will be logged in #commlog"
            )("PMs")
        ,
        (priv && Granter(_.FullCommsExport)).option:
          postForm(action := routes.Mod.fullCommsExport(u.username)):
            form3.action(
              form3.submit(
                "Full comms export",
                icon = none,
                confirm =
                  s"Confirm you want to export all comms from **${u.username}** (including other party)".some
              )(cls := "button-red button-empty comms-export")
            )
      )
    )

  def publicChats(u: User, publicLines: List[PublicLine], sourceOf: PublicSource => Translate ?=> Tag)(using
      Context
  ) =
    publicLines.nonEmpty.option:
      frag(
        h2("Recent public chats"),
        div(cls := "player_chats"):
          publicLines
            .groupBy(_.from)
            .toList
            .flatMap: (source, lines) =>
              lines.toNel.map(source -> _)
            .sortBy(_._2.head.date)(Ordering[Instant].reverse)
            .map: (source, lines) =>
              div(cls := "game")(
                sourceOf(source)(cls := "title")(
                  " – ",
                  momentFromNowServer(lines.head.date)
                ),
                div(cls := "chat"):
                  lines.toList.map: line =>
                    div(cls := "line author")(
                      userIdLink(u.some, withOnline = false, withTitle = false),
                      nbsp,
                      span(cls := "message")(highlightBad(line.text))
                    )
              )
      )

  def privateChats(u: User, players: List[(Pov, MixedChat)])(using Context) = frag(
    h2("Recent private chats"),
    players.nonEmpty.option:
      div(cls := "player_chats")(
        players.map: (pov, chat) =>
          div(cls := "game")(
            a(
              href := routes.Round.player(pov.fullId),
              cls := List(
                "title"        -> true,
                "friend_title" -> pov.game.sourceIs(_.Friend)
              ),
              title := pov.game.sourceIs(_.Friend).option("Friend game")
            )(
              titleNameOrAnon(pov.opponent.userId),
              " – ",
              momentFromNowServer(pov.game.movedAt)
            ),
            div(cls := "chat")(
              chat.lines.map: line =>
                div(
                  cls := List(
                    "line"   -> true,
                    "author" -> UserStr(line.author).is(u)
                  )
                )(
                  userIdLink(line.userIdMaybe, withOnline = false, withTitle = false),
                  nbsp,
                  span(cls := "message")(highlightBad(line.text))
                )
            )
          )
      )
  )
