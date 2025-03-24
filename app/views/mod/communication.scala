package views.mod

import lila.app.UiEnv.{ *, given }
import lila.core.shutup.{ PublicLine, PublicSource }
import lila.mod.IpRender.RenderIp
import lila.mod.UserWithModlog
import lila.shutup.Analyser

def communication(
    mod: Me,
    timeline: lila.api.ModTimeline,
    players: List[(Pov, lila.chat.MixedChat)],
    convos: List[lila.msg.ModMsgConvo],
    publicLines: List[PublicLine],
    logins: lila.security.UserLogins.TableData[UserWithModlog],
    appeals: List[lila.appeal.Appeal],
    priv: Boolean
)(using Context, Me, RenderIp) =
  val u = timeline.user
  Page(s"${u.username} communications")
    .css("mod.communication")
    .css(isGranted(_.UserModView).option("mod.user"))
    .js(isGranted(_.UserModView).option(Esm("mod.user"))):
      main(id := "communication", cls := "box box-pad")(
        commUi.commsHeader(u, priv),
        isGranted(_.UserModView).option:
          frag(
            div(cls := "mod-zone mod-zone-full none"),
            views.user.mod.otherUsers(mod, u, logins, appeals)(
              cls := "mod-zone communication__logins"
            )
          )
        ,
        views.mod.timeline.renderComm(timeline),
        priv.option(
          frag(
            commUi.privateChats(u, players),
            h2("Recent inbox messages"),
            div(cls := "threads")(
              convos.nonEmpty.option:
                convos.map: modConvo =>
                  div(cls := "thread")(
                    p(cls := "title")(
                      strong(userLink(modConvo.contact)),
                      modConvo.contact.marks.troll.option:
                        span(cls := "user_marks")(iconTag(Icon.BubbleSpeech))
                      ,
                      modConvo.relations.in
                        .exists(_.isFollow)
                        .option:
                          span(cls := "friend_title")(
                            "is following this user",
                            br
                          )
                    ),
                    table(cls := "slist")(
                      tbody(
                        modConvo.truncated.option:
                          div(cls := "truncated-convo")(
                            s"Truncated, showing last ${modConvo.msgs.length} messages"
                          )
                        ,
                        modConvo.msgs.reverse.map: msg =>
                          val author = msg.user.is(u)
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
        ),
        commUi.publicChats(u, publicLines, publicLineSource)
      )
