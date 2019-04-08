package views.html
package relay

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue
import lila.game.Pov

import controllers.routes

object show {

  def apply(
    r: lila.relay.Relay,
    s: lila.study.Study,
    data: lila.relay.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: lila.socket.Socket.SocketVersion,
    streams: List[lila.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    title = r.name,
    // side = Some(frag(
    //   div(cls := "side_box study_box"),
    //   streams.map { s =>
    //     a(href := routes.Streamer.show(s.streamer.userId), cls := "context-streamer text side_box", dataIcon := "")(
    //       usernameOrId(s.streamer.userId),
    //       " is streaming"
    //     )
    //   }
    // )),
    // chat = chat.frag.some,
    // underchat = Some(views.html.game.bits.watchers),
    moreCss = cssTags("analyse.css", "study.css", "relay.css", "chat.css"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lichess = lichess || {}; lichess.relay = {
relay: ${safeJsonValue(data.relay)},
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${board.userAnalysisI18n()},
tagTypes: '${lila.study.PgnTags.typesToString}',
userId: $jsUserIdString,
chat: ${
        chatOption.fold("null")(c => safeJsonValue(chat.json(
          c.chat,
          name = trans.chatRoom.txt(),
          timeout = c.timeout,
          writeable = ctx.userId.??(s.canChat),
          public = false,
          localMod = ctx.userId.??(s.canContribute)
        )))
      },
explorer: {
endpoint: "$explorerEndpoint",
tablebaseEndpoint: "$tablebaseEndpoint"
},
socketUrl: "${routes.Relay.websocket(s.id.value, apiVersion.value)}",
socketVersion: $socketVersion
};""")
    ),
    chessground = false,
    zoomable = true,
    openGraph = lila.app.ui.OpenGraph(
      title = r.name,
      url = s"$netBaseUrl${routes.Relay.show(r.slug, r.id.value).url}",
      description = shorten(r.description, 152)
    ).some
  ) {
      div(cls := "analyse cg-512")(
        board.bits.domPreload(none)
      )
    }
}
