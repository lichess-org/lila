package views.html.simul

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    sim: lila.simul.Simul,
    socketVersion: lila.socket.Socket.SocketVersion,
    data: play.api.libs.json.JsObject,
    chatOption: Option[lila.chat.UserChat.Mine],
    stream: Option[lila.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    responsive = true,
    moreCss = responsiveCssTag("simul.show"),
    title = sim.fullName,
    underchat = Some(div(
      cls := "watchers none",
      aria.live := "off",
      aria.relevant := "additions removals text"
    )(span(cls := "list inline_userlist"))),
    chat = views.html.chat.frag.some,
    moreJs = frag(
      jsAt(s"compiled/lichess.simul${isProd ?? (".min")}.js"),
      embedJs(s"""lichess.simul={
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
              })
            ),
            trans.by.frag(usernameOrId(sim.hostId)),
            " ",
            momentFromNow(sim.createdAt)
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
