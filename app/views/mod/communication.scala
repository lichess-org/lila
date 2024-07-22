package views.mod

import lila.app.UiEnv.{ *, given }
import lila.common.String.html.richText
import lila.core.shutup.PublicSource
import lila.mod.IpRender.RenderIp
import lila.mod.UserWithModlog
import lila.shutup.Analyser

def communication(
    mod: Me,
    u: User,
    players: List[(Pov, lila.chat.MixedChat)],
    convos: List[lila.msg.ModMsgConvo],
    publicLines: List[lila.shutup.PublicLine],
    notes: List[lila.user.Note],
    history: List[lila.mod.Modlog],
    logins: lila.security.UserLogins.TableData[UserWithModlog],
    reports: List[lila.report.Report],
    appeals: List[lila.appeal.Appeal],
    priv: Boolean
)(using ctx: Context, renderIp: RenderIp) =
  Page(s"${u.username} communications")
    .css("mod.communication")
    .css(isGranted(_.UserModView).option("mod.user"))
    .js(isGranted(_.UserModView).option(EsmInit("mod.user"))):
      main(id := "communication", cls := "box box-pad")(
        boxTop(
          h1(
            div(cls := "title")(userLink(u), " communications"),
            div(cls := "actions")(
              a(
                cls  := "button button-empty mod-zone-toggle",
                href := routes.User.mod(u.username),
                titleOrText("Mod zone (Hotkey: m)"),
                dataIcon := Icon.Agent
              ),
              isGranted(_.ViewPrivateComms)
                .option {
                  if priv then
                    a(cls := "priv button active", href := routes.Mod.communicationPublic(u.username))("PMs")
                  else
                    a(
                      cls   := "priv button",
                      href  := routes.Mod.communicationPrivate(u.username),
                      title := "View private messages. This will be logged in #commlog"
                    )("PMs")
                },
              (priv && isGranted(_.FullCommsExport))
                .option {
                  postForm(
                    action := routes.Mod.fullCommsExport(u.username)
                  )(
                    form3.action(
                      form3.submit(
                        "Full comms export",
                        icon = none,
                        confirm =
                          s"Confirm you want to export all comms from **${u.username}** (including other party)".some
                      )(cls := "button-red button-empty comms-export")
                    )
                  )
                }
            )
          )
        ),
        isGranted(_.UserModView).option(
          frag(
            div(cls := "mod-zone mod-zone-full none"),
            views.user.mod.otherUsers(mod, u, logins, appeals)(
              cls := "mod-zone communication__logins"
            )
          )
        ),
        reports.nonEmpty.option(
          frag(
            h2("Comm reports"),
            div(cls := "reports history")(
              reports.map: r =>
                div(
                  h3(r.reason.name),
                  r.atoms.toList.map: a =>
                    div(
                      userIdLink(a.by.some),
                      " ",
                      momentFromNowServer(a.at),
                      ": ",
                      richText(a.text)
                    )
                )
            )
          )
        ),
        history.nonEmpty.option(
          frag(
            h2("Moderation history"),
            div(cls := "history")(
              history.map: e =>
                div(
                  userIdLink(e.mod.userId.some),
                  " ",
                  b(e.showAction),
                  " ",
                  u.username,
                  " ",
                  e.details,
                  " ",
                  momentFromNowServer(e.date)
                )
            )
          )
        ),
        notes.nonEmpty.option(
          frag(
            h2("Notes from other users"),
            div(cls := "notes")(
              notes.map: note =>
                (isGranted(_.Admin) || !note.dox).option(
                  div(
                    userIdLink(note.from.some),
                    " ",
                    momentFromNowServer(note.date),
                    ": ",
                    richText(note.text)
                  )
                )
            )
          )
        ),
        h2("Dubious public chats"),
        if publicLines.isEmpty then strong("None!")
        else
          ul(cls := "public_chats")(
            publicLines.reverse.map: line =>
              li(cls := "line author")(
                line.date.fold[Frag]("[OLD]")(momentFromNowServer),
                " ",
                line.from.map:
                  case PublicSource.Tournament(id) => views.tournament.ui.tournamentLink(id)
                  case PublicSource.Simul(id)      => views.simul.ui.link(id)
                  case PublicSource.Team(id)       => teamLink(id)
                  case PublicSource.Watcher(id) =>
                    a(href := routes.Round.watcher(id, Color.white))("Game #", id)
                  case PublicSource.Study(id) => a(href := routes.Study.show(id))("Study #", id)
                  case PublicSource.Swiss(id) => views.swiss.ui.link(id)
                  case PublicSource.Forum(id) => a(href := routes.ForumPost.redirect(id))("Forum #", id)
                  case PublicSource.Ublog(id) => a(href := routes.Ublog.redirect(id))("User blog #", id)
                ,
                nbsp,
                span(cls := "message")(Analyser.highlightBad(line.text))
              )
          )
        ,
        priv.option(
          frag(
            h2("Recent private chats"),
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
                          "author" -> (UserStr(line.author).is(u))
                        )
                      )(
                        userIdLink(line.userIdMaybe, withOnline = false, withTitle = false),
                        nbsp,
                        span(cls := "message")(Analyser.highlightBad(line.text))
                      )
                  )
                )
            ),
            div(cls := "threads")(
              h2("Recent inbox messages"),
              convos.map: modConvo =>
                div(cls := "thread")(
                  p(cls := "title")(
                    strong(userLink(modConvo.contact)),
                    showSbMark(modConvo.contact),
                    modConvo.relations.in
                      .exists(_.isFollow)
                      .option(
                        span(cls := "friend_title")(
                          "is following this user",
                          br
                        )
                      )
                  ),
                  table(cls := "slist")(
                    tbody(
                      modConvo.truncated.option(
                        div(cls := "truncated-convo")(
                          s"Truncated, showing last ${modConvo.msgs.length} messages"
                        )
                      ),
                      modConvo.msgs.reverse.map: msg =>
                        val author = msg.user == u.id
                        tr(cls := List("post" -> true, "author" -> author))(
                          td(momentFromNowServer(msg.date)),
                          td(strong(if author then u.username else modConvo.contact.username)),
                          td(cls := "message")(Analyser.highlightBad(msg.text))
                        )
                    )
                  )
                )
            )
          )
        )
      )

private def showSbMark(u: User) =
  u.marks.troll.option(span(cls := "user_marks")(iconTag(Icon.BubbleSpeech)))
