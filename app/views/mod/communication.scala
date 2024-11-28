package views.mod

import lila.app.UiEnv.{ *, given }
import lila.core.shutup.PublicSource
import lila.mod.IpRender.RenderIp
import lila.mod.UserWithModlog
import lila.shutup.Analyser

def publicLineSource(source: lila.core.shutup.PublicSource)(using Translate): Frag = source match
  case PublicSource.Tournament(id) => views.tournament.ui.tournamentLink(id)
  case PublicSource.Simul(id)      => views.simul.ui.link(id)
  case PublicSource.Team(id)       => teamLink(id)
  case PublicSource.Watcher(id) =>
    a(href := routes.Round.watcher(id, Color.white))("Game #", id)
  case PublicSource.Study(id) => a(href := routes.Study.show(id))("Study #", id)
  case PublicSource.Swiss(id) => views.swiss.ui.link(id)
  case PublicSource.Forum(id) => a(href := routes.ForumPost.redirect(id))("Forum #", id)
  case PublicSource.Ublog(id) => a(href := routes.Ublog.redirect(id))("User blog #", id)
  case PublicSource.Relay(id) => a(href := routes.RelayRound.show("-", "-", id))("Broadcast #", id)

def communication(
    mod: Me,
    timeline: lila.api.ModTimeline,
    players: List[(Pov, lila.chat.MixedChat)],
    convos: List[lila.msg.ModMsgConvo],
    publicLines: List[lila.shutup.PublicLine],
    logins: lila.security.UserLogins.TableData[UserWithModlog],
    appeals: List[lila.appeal.Appeal],
    priv: Boolean
)(using ctx: Context, renderIp: RenderIp) =
  val u = timeline.user
  Page(s"${u.username} communications")
    .css("mod.communication")
    .css(isGranted(_.UserModView).option("mod.user"))
    .js(isGranted(_.UserModView).option(Esm("mod.user"))):
      main(id := "communication", cls := "box box-pad")(
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
        ),
        isGranted(_.UserModView).option(
          frag(
            div(cls := "mod-zone mod-zone-full none"),
            views.user.mod.otherUsers(mod, u, logins, appeals)(
              cls := "mod-zone communication__logins"
            )
          )
        ),
        views.mod.timeline.renderComm(timeline),
        priv.option(
          frag(
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
                            "author" -> (UserStr(line.author).is(u))
                          )
                        )(
                          userIdLink(line.userIdMaybe, withOnline = false, withTitle = false),
                          nbsp,
                          span(cls := "message")(Analyser.highlightBad(line.text))
                        )
                    )
                  )
              )
            ,
            h2("Recent inbox messages"),
            div(cls := "threads")(
              convos.nonEmpty.option:
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
