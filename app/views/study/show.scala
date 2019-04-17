package views.html.study

import play.api.libs.json.Json

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    s: lidraughts.study.Study,
    data: lidraughts.study.JsonView.JsData,
    chatOption: Option[lidraughts.chat.UserChat.Mine],
    socketVersion: lidraughts.socket.Socket.SocketVersion,
    streams: List[lidraughts.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s.name.value,
    moreCss = responsiveCssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJs(s"""lidraughts=window.lidraughts||{};lidraughts.study={
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${views.html.board.userAnalysisI18n()},
tagTypes: '${lidraughts.study.PdnTags.typesToString}',
userId: $jsUserIdString,
chat: ${
        chatOption.fold("null")(c => safeJsonValue(views.html.chat.json(
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
socketUrl: "${routes.Study.websocket(s.id.value, apiVersion.value)}",
socketVersion: $socketVersion
};""")
    ),
    robots = s.isPublic,
    draughtsground = false,
    zoomable = true,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
      description = s"A draughts study by ${usernameOrId(s.ownerId)}"
    ).some
  ) {
      main(cls := "analyse")(
        div(cls := "analyse__side")(
          div(cls := "study__side"),
          streams.map { s =>
            views.html.streamer.bits.contextual(s.streamer.userId)
          }
        )
      )
    }
}
