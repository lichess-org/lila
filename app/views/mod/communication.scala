package views.html.mod

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText
import lidraughts.hub.actorApi.shutup.PublicSource

import controllers.routes

object communication {

  def apply(
    u: lidraughts.user.User,
    players: List[(lidraughts.game.Pov, lidraughts.chat.MixedChat)],
    threads: List[lidraughts.message.Thread],
    publicLines: List[lidraughts.shutup.PublicLine],
    spy: lidraughts.security.UserSpy,
    otherWithEmails: lidraughts.security.UserSpy.WithMeSortedWithEmails,
    notes: List[lidraughts.user.Note],
    history: List[lidraughts.mod.Modlog],
    priv: Boolean
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = u.username + " communications",
      moreCss = cssTag("mod.communication")
    ) {
        main(id := "communication", cls := "box box-pad")(
          h1(
            div(cls := "title")(userLink(u), " communications"),
            div(cls := "actions")(
              isGranted(_.ViewPrivateComms) option {
                if (priv) a(cls := "priv button active", href := routes.Mod.communicationPublic(u.username))("PMs")
                else a(
                  cls := "priv button",
                  href := routes.Mod.communicationPrivate(u.username),
                  title := "View private messages. This will be logged in #commlog"
                )("PMs")
              }
            )
          ),

          history.nonEmpty option frag(
            h2("Moderation history"),
            div(cls := "history")(
              history.map { e =>
                div(userIdLink(e.mod.some), " ", b(e.showAction), " ", u.username, " ", e.details, " ", momentFromNowOnce(e.date))
              }
            )
          ),

          notes.nonEmpty option frag(
            h2("Notes from other users"),
            div(cls := "notes")(
              notes.map { note =>
                div(userIdLink(note.from.some), " ", momentFromNowOnce(note.date), ": ", richText(note.text))
              }
            )
          ),

          h2("Dubious public chats"),
          if (publicLines.isEmpty) strong("None!")
          else ul(cls := "public_chats")(
            publicLines.reverse.map { line =>
              li(
                line.date.map(momentFromNowOnce(_)).getOrElse("[OLD]"),
                line.from.map {
                  case PublicSource.Tournament(id) => tournamentLink(id)
                  case PublicSource.Simul(id) => views.html.simul.bits.link(id)
                  case PublicSource.Watcher(id) => a(href := routes.Round.watcher(id, "white"))("Game #", id)
                  case PublicSource.Study(id) => a(href := routes.Study.show(id))("Study #", id)
                },
                line.text
              )
            }
          ),

          priv option frag(
            h2("Recent private chats"),
            div(cls := "player_chats")(
              players.map {
                case (pov, chat) => div(cls := "game")(
                  a(
                    href := routes.Round.player(pov.fullId),
                    cls := List(
                      "title" -> true,
                      "friend_title" -> pov.game.fromFriend
                    ),
                    title := pov.game.fromFriend.option("Friend game")
                  )(
                      usernameOrAnon(pov.opponent.userId), " – ", momentFromNowOnce(pov.game.movedAt)
                    ),
                  div(cls := "chat")(
                    chat.lines.map { line =>
                      div(cls := List(
                        "line" -> true,
                        "author" -> (line.author.toLowerCase == u.id)
                      ))(
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
              threads.map { thread =>
                div(cls := "thread")(
                  p(cls := "title")(
                    strong(thread.name),
                    momentFromNowOnce(thread.createdAt),
                    userIdLink(thread.creatorId.some), " -> ", userIdLink(thread.invitedId.some)
                  ),
                  thread.posts.map { post =>
                    div(cls := List("post" -> true, "author" -> thread.isWrittenBy(post, u)))(
                      userIdLink(thread.senderOf(post).some),
                      nbsp,
                      richText(post.text)
                    )
                  }
                )
              }
            )
          ),
          div(cls := "alternate_accounts")(
            h2("Alternate accounts"),
            table(cls := "others slist")(
              thead(
                tr(
                  th(spy.otherUsers.size, " similar user(s)"),
                  th("Email"),
                  th("Same"),
                  th("Games"),
                  th("Marks"),
                  th("IPban"),
                  th("Closed"),
                  th("Created")
                )
              ),
              tbody(
                otherWithEmails.others.map {
                  case lidraughts.security.UserSpy.OtherUser(o, byIp, byFp) => tr(cls := (o == u).option("same"))(
                    td(userLink(o, withBestRating = true, params = "?mod")),
                    td(otherWithEmails emailValueOf o),
                    td(
                      if (o == u) " - "
                      else List(byIp option "IP", byFp option "Print").flatten.mkString(", ")
                    ),
                    td(o.count.game.localize),
                    td(
                      o.engine option "ENGINE",
                      o.booster option "BOOSTER",
                      o.troll option "SHADOWBAN"
                    ),
                    td(o.ipBan option "IPBAN"),
                    td(o.disabled option "CLOSED"),
                    td(momentFromNowOnce(o.createdAt))
                  )
                }
              )
            )
          )
        )
      }
}
