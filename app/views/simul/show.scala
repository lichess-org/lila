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
    moreCss = cssTag("simul.show"),
    title = sim.fullName,
    moreJs = frag(
      jsAt(s"compiled/lichess.simul${isProd ?? (".min")}.js"),
      embedJsUnsafe(s"""lichess.simul={
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
                    trans.casual()
                  )
                )
              ),
              trans.simulHostExtraTime(),
              ": ",
              pluralize("minute", sim.clock.hostExtraMinutes),
              br,
              trans.hostColorX(sim.color match {
                case Some("white") => trans.white()
                case Some("black") => trans.black()
                case _ => trans.randomColor()
              })
            ),
            trans.by(usernameOrId(sim.hostId)),
            " ",
            momentFromNow(sim.createdAt)
          ),
          stream.map { s =>
            views.html.streamer.bits.contextual(s.streamer.userId)
          },
          chatOption.isDefined option views.html.chat.frag
        ),
        div(cls := "simul__main box")(spinner)
      )
    }
}
