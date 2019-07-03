package views.html
package relay

import play.api.libs.json.Json

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
    moreCss = cssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lichess=window.lichess||{};lichess.relay=${
        safeJsonValue(Json.obj(
          "relay" -> data.relay,
          "study" -> data.study,
          "data" -> data.analysis,
          "i18n" -> (board.userAnalysisI18n(withAdvantageChart = true) ++ i18nFullDbJsObject(lila.i18n.I18nDb.Study)),
          "tagTypes" -> lila.study.PgnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map(c => chat.json(
            c.chat,
            name = trans.chatRoom.txt(),
            timeout = c.timeout,
            writeable = ctx.userId.??(s.canChat),
            public = false,
            localMod = ctx.userId.??(s.canContribute)
          )),
          "explorer" -> Json.obj(
            "endpoint" -> explorerEndpoint,
            "tablebaseEndpoint" -> tablebaseEndpoint
          ),
          "socketUrl" -> routes.Relay.websocket(s.id.value, apiVersion.value).url,
          "socketVersion" -> socketVersion.value
        ))
      }""")
    ),
    chessground = false,
    zoomable = true,
    csp = defaultCsp.withTwitch.some,
    openGraph = lila.app.ui.OpenGraph(
      title = r.name,
      url = s"$netBaseUrl${routes.Relay.show(r.slug, r.id.value).url}",
      description = shorten(r.description, 152)
    ).some
  )(frag(
      main(cls := "analyse"),
      views.html.study.bits.streamers(streams)
    ))

  def widget(r: lila.relay.Relay.WithStudyAndLiked, extraCls: String = "")(implicit ctx: Context) =
    div(cls := s"relay-widget $extraCls", dataIcon := "")(
      a(cls := "overlay", href := routes.Relay.show(r.relay.slug, r.relay.id.value)),
      div(
        h3(r.relay.name),
        p(r.relay.description)
      )
    )
}
