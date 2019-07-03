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
    moreCss = cssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lichess=window.lichess||{};lichess.study=${
        safeJsonValue(Json.obj(
          "study" -> data.study,
          "data" -> data.analysis,
          "i18n" -> (views.html.board.userAnalysisI18n(withAdvantageChart = true) ++ i18nFullDbJsObject(lila.i18n.I18nDb.Study)),
          "tagTypes" -> lila.study.PgnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map { c =>
            views.html.chat.json(
              c.chat,
              name = trans.chatRoom.txt(),
              timeout = c.timeout,
              writeable = ctx.userId.??(s.canChat),
              public = false,
              localMod = ctx.userId.??(s.canContribute)
            )
          },
          "explorer" -> Json.obj(
            "endpoint" -> explorerEndpoint,
            "tablebaseEndpoint" -> tablebaseEndpoint
          ),
          "socketUrl" -> routes.Study.websocket(s.id.value, apiVersion.value).url,
          "socketVersion" -> socketVersion.value
        ))
      }""")
    ),
    robots = s.isPublic,
    chessground = false,
    zoomable = true,
    csp = defaultCsp.withTwitch.some,
    openGraph = lila.app.ui.OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
      description = s"A chess study by ${usernameOrId(s.ownerId)}"
    ).some
  )(frag(
      main(cls := "analyse"),
      bits.streamers(streams)
    ))
}
