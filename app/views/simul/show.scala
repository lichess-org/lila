package views.html.simul

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    sim: lidraughts.simul.Simul,
    socketVersion: lidraughts.socket.Socket.SocketVersion,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    stream: Option[lidraughts.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    responsive = true,
    moreCss = responsiveCssTag("simul.show"),
    title = sim.fullName,
    // underchat = Some(div(
    //   cls := "watchers none",
    //   aria.live := "off",
    //   aria.relevant := "additions removals text"
    // )(span(cls := "list inline_userlist"))),
    moreJs = frag(
      jsAt(s"compiled/lidraughts.simul${isProd ?? (".min")}.js"),
      embedJs(s"""lidraughts.simul={
data:${safeJsonValue(data)},
i18n:${bits.jsI18n()},
socketVersion:${socketVersion.value},
userId: $jsUserIdString,
chat: ${chatOption.fold("null")(c => safeJsonValue(views.html.chat.json(c.chat, name = trans.chatRoom.txt(), timeout = c.timeout, public = true)))}}""")
    )
  ) {
      main(cls := "simul")(
        st.aside(cls := "simul__side")(
          div(cls := "simul__meta")(
            div(cls := "game-infos")(
              div(cls := "header")(
                div(cls := List(
                  "variant-icons" -> true,
                  "rich" -> sim.variantRich
                ))(sim.perfTypes.map { pt => span(dataIcon := pt.iconChar) }),
                div(
                  span(cls := "clock")(sim.clock.config.show),
                  div(cls := "setup")(
                    sim.variants.map(_.name).mkString(", "),
                    " • ",
                    trans.casual.frag()
                  )
                )
              ),
              trans.simulHostExtraTime.frag(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes),
              br,
              trans.hostColorX.frag(sim.color match {
                case Some("white") => trans.white.frag()
                case Some("black") => trans.black.frag()
                case _ => trans.randomColor.frag()
              }),
              sim.spotlight.flatMap(_.drawLimit).map { lim =>
                frag(
                  br,
                  if (lim > 0) trans.drawOffersAfterX.frag(lim)
                  else trans.drawOffersNotAllowed.frag()
                )
              },
              sim.targetPct.map { target =>
                frag(
                  br,
                  trans.targetWinningPercentage.frag(s"$target%")
                )
              },
              (sim.chatmode.isDefined && !sim.chatmode.contains(lidraughts.simul.Simul.ChatMode.Everyone)) option {
                frag(
                  br,
                  trans.chatAvailableForX.frag(sim.chatmode match {
                    case Some(lidraughts.simul.Simul.ChatMode.Spectators) => trans.spectatorsOnly.frag()
                    case _ => trans.participantsOnly.frag()
                  })
                )
              },
              sim.allowed.filter(_.nonEmpty).map { allowed =>
                frag(
                  br,
                  trans.simulParticipationLimited.frag(allowed.size)
                )
              }
            ),
            trans.by.frag(usernameOrId(sim.hostId)),
            " ",
            sim.spotlight.fold(momentFromNow(sim.createdAt)) { s => absClientDateTime(s.startsAt) }
          ),
          stream.map { s =>
            a(
              href := routes.Streamer.show(s.streamer.userId),
              cls := "context-streamer text side_box",
              dataIcon := ""
            )(usernameOrId(s.streamer.userId), " is streaming")
          },
          chatOption.isDefined option views.html.chat.frag
        ),
        div(cls := "simul__main box")(spinner)
      )
    }
}
