package views.html.study

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
    s: lila.study.Study,
    data: lila.study.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: lila.socket.Socket.SocketVersion,
    streams: List[lila.streamer.Stream]
  )(implicit ctx: Context) = views.html.base.layout(
    title = s.name.value,
    side = Some(div(cls := "side_box study_box")(
      streams.map { s =>
        a(
          href := routes.Streamer.show(s.streamer.userId),
          cls := "context-streamer text side_box",
          dataIcon := ""
        )(
            usernameOrId(s.streamer.userId),
            " is streaming"
          )
      }
    )),
    chat = views.html.chat.html.some,
    underchat = Some(views.html.game.bits.watchers),
    moreCss = cssTags("analyse.css", "study.css", "chat.css"),
    moreJs = frag(
      jsAt(s"compiled/lichess.analyse${isProd ?? (".min")}.js"),
      embedJs(s"""lichess=lichess||{};lichess.study={
study: ${safeJsonValue(data.study)},
data: ${safeJsonValue(data.analysis)},
i18n: ${views.html.board.userAnalysisI18n()},
tagTypes: '${lila.study.PgnTags.typesToString}',
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
    chessground = false,
    zoomable = true,
    openGraph = lila.app.ui.OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
      description = s"A chess study by ${usernameOrId(s.ownerId)}"
    ).some
  ) {
      div(cls := "analyse cg-512")(views.html.board.bits.domPreload(none))
    }
}
