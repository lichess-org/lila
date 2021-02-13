package views.html.mod

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.richText
import lila.hub.actorApi.shutup.PublicSource

import controllers.routes

object communication {

  def apply(
      u: lila.user.User,
      players: List[(lila.game.Pov, lila.chat.MixedChat)],
      convos: List[lila.msg.MsgConvo],
      publicLines: List[lila.shutup.PublicLine],
      notes: List[lila.user.Note],
      history: List[lila.mod.Modlog],
      priv: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = u.username + " communications",
      moreCss = frag(
        cssTag("mod.communication"),
        isGranted(_.UserSpy) option cssTag("mod.user")
      ),
      moreJs = frag(
        isGranted(_.UserSpy) option jsModule("mod.user")
      )
    ) {
      main(id := "communication", cls := "box box-pad")(
        h1(
          div(cls := "title")(userLink(u), " communications"),
          div(cls := "actions")(
            a(
              cls := "button button-empty mod-zone-toggle",
              href := routes.User.mod(u.username),
              titleOrText("Mod zone (Hotkey: m)"),
              dataIcon := ""
            ),
            isGranted(_.ViewPrivateComms) option {
              if (priv)
                a(cls := "priv button active", href := routes.Mod.communicationPublic(u.username))("PMs")
              else
                a(
                  cls := "priv button",
                  href := routes.Mod.communicationPrivate(u.username),
                  title := "View private messages. This will be logged in #commlog"
                )("PMs")
            }
          )
        ),
        isGranted(_.UserSpy) option div(cls := "mod-zone none"),
        history.nonEmpty option frag(
          h2("Moderation history"),
          div(cls := "history")(
            history.map { e =>
              div(
                userIdLink(e.mod.some),
                " ",
                b(e.showAction),
                " ",
                u.username,
                " ",
                e.details,
                " ",
                momentFromNowOnce(e.date)
              )
            }
          )
        ),
        notes.nonEmpty option frag(
          h2("Notes from other users"),
          div(cls := "notes")(
            notes.map { note =>
              (isGranted(_.Doxing) || !note.dox) option
                div(userIdLink(note.from.some), " ", momentFromNowOnce(note.date), ": ", richText(note.text))
            }
          )
        ),
        h2("Dubious public chats"),
        if (publicLines.isEmpty) strong("None!")
        else
          ul(cls := "public_chats")(
            publicLines.reverse.map { line =>
              li(
                line.date.fold[Frag]("[OLD]")(momentFromNowOnce),
                " ",
                line.from.map {
                  case PublicSource.Tournament(id) => tournamentLink(id)
                  case PublicSource.Simul(id)      => views.html.simul.bits.link(id)
                  case PublicSource.Team(id)       => views.html.team.bits.link(id)
                  case PublicSource.Watcher(id)    => a(href := routes.Round.watcher(id, "white"))("Game #", id)
                  case PublicSource.Study(id)      => a(href := routes.Study.show(id))("Study #", id)
                  case PublicSource.Swiss(id)      => views.html.swiss.bits.link(lila.swiss.Swiss.Id(id))
                },
                " ",
                line.text
              )
            }
          ),
        priv option frag(
          h2("Recent private chats"),
          div(cls := "player_chats")(
            players.map {
              case (pov, chat) =>
                div(cls := "game")(
                  a(
                    href := routes.Round.player(pov.fullId),
                    cls := List(
                      "title"        -> true,
                      "friend_title" -> pov.game.fromFriend
                    ),
                    title := pov.game.fromFriend.option("Friend game")
                  )(
                    usernameOrAnon(pov.opponent.userId),
                    " – ",
                    momentFromNowOnce(pov.game.movedAt)
                  ),
                  div(cls := "chat")(
                    chat.lines.map { line =>
                      div(
                        cls := List(
                          "line"   -> true,
                          "author" -> (line.author.toLowerCase == u.id)
                        )
                      )(
                        userIdLink(line.author.toLowerCase.some, withOnline = false, withTitle = false),
                        nbsp,
                        richText(line.text)
                      )
                    }
                  )
                )
            }
          ),
          div(cls := "threads")(
            h2("Recent inbox messages"),
            convos.map { convo =>
              div(cls := "thread")(
                p(cls := "title")(strong(lightUserLink(convo.contact))),
                table(cls := "slist")(
                  tbody(
                    convo.msgs.reverse.map { msg =>
                      val author = msg.user == u.id
                      tr(cls := List("post" -> true, "author" -> author))(
                        td(momentFromNowOnce(msg.date)),
                        td(strong(if (author) u.username else convo.contact.name)),
                        td(richText(msg.text))
                      )
                    }
                  )
                )
              )
            }
          )
        )
      )
    }
}
