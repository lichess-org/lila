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
    moreCss = cssTag("analyse.study"),
    moreJs = frag(
      analyseTag,
      analyseNvuiTag,
      embedJsUnsafe(s"""lidraughts=window.lidraughts||{};lidraughts.study=${
        safeJsonValue(Json.obj(
          "study" -> data.study,
          "data" -> data.analysis,
          "i18n" -> jsI18n(),
          "tagTypes" -> lidraughts.study.PdnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map { c =>
            views.html.chat.json(
              c.chat,
              name = trans.chatRoom.txt(),
              timeout = c.timeout,
              writeable = ctx.userId.??(s.canChat),
              public = false,
              resourceId = lidraughts.chat.Chat.ResourceId(s"study/${c.chat.id}"),
              palantir = ctx.userId ?? s.isMember,
              localMod = ctx.userId ?? s.canContribute
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
    draughtsground = false,
    zoomable = true,
    csp = defaultCsp.withTwitch.withPeer.some,
    openGraph = lidraughts.app.ui.OpenGraph(
      title = s.name.value,
      url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
      description = s"A draughts study by ${usernameOrId(s.ownerId)}"
    ).some
  )(frag(
      main(cls := "analyse"),
      bits.streamers(streams)
    ))
}
