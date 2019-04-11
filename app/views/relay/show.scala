package views.html
package relay

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue
import lidraughts.game.Pov

import controllers.routes

object show {

  def apply(
    r: lidraughts.relay.Relay,
    s: lidraughts.study.Study,
    data: lidraughts.relay.JsonView.JsData,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    socketVersion: lidraughts.socket.Socket.SocketVersion,
    streams: List[lidraughts.streamer.Stream]
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
    responsive = true,
    moreCss = responsiveCssTag("analyse.relay"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lidraughts=window.lidraughts||{};lidraughts.relay={
relay: ${safeJsonValue(data.relay)},
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${board.userAnalysisI18n()},
tagTypes: '${lidraughts.study.PdnTags.typesToString}',
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
    draughtsground = false,
    zoomable = true,
    csp = defaultCsp.withTwitch.some,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = r.name,
      url = s"$netBaseUrl${routes.Relay.show(r.slug, r.id.value).url}",
      description = shorten(r.description, 152)
    ).some
  ) {
      main(cls := "analyse")
    }

  def widget(r: lidraughts.relay.Relay.WithStudyAndLiked, extraCls: String = "")(implicit ctx: Context) =
    div(cls := s"relay-widget $extraCls", dataIcon := "")(
      a(cls := "overlay", href := routes.Relay.show(r.relay.slug, r.relay.id.value)),
      div(
        h3(r.relay.name),
        p(r.relay.description)
      )
    )
}
