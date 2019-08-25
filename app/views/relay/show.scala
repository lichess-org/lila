package views.html
package relay

import play.api.libs.json.Json

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
    moreCss = cssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lidraughts=window.lidraughts||{};lidraughts.relay=${
        safeJsonValue(Json.obj(
          "relay" -> data.relay,
          "study" -> data.study,
          "data" -> data.analysis,
          "i18n" -> (board.userAnalysisI18n(withAdvantageChart = true) ++ i18nFullDbJsObject(lidraughts.i18n.I18nDb.Study)),
          "tagTypes" -> lidraughts.study.PdnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map(c => chat.json(
            c.chat,
            name = trans.chatRoom.txt(),
            timeout = c.timeout,
            writeable = ctx.userId.??(s.canChat),
            public = false,
            resourceId = lidraughts.chat.Chat.ResourceId(s"relay/${c.chat.id}"),
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
    draughtsground = false,
    zoomable = true,
    csp = defaultCsp.withTwitch.some,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = r.name,
      url = s"$netBaseUrl${routes.Relay.show(r.slug, r.id.value).url}",
      description = shorten(r.description, 152)
    ).some
  )(frag(
      main(cls := "analyse"),
      views.html.study.bits.streamers(streams)
    ))

  def widget(r: lidraughts.relay.Relay.WithStudyAndLiked, extraCls: String = "")(implicit ctx: Context) =
    div(cls := s"relay-widget $extraCls", dataIcon := "")(
      a(cls := "overlay", href := routes.Relay.show(r.relay.slug, r.relay.id.value)),
      div(
        h3(r.relay.name),
        p(r.relay.description)
      )
    )
}
