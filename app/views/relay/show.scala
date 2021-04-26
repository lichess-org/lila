package views.html
package relay

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes

object show {

  def apply(
      rt: lila.relay.RelayRound.WithTourAndStudy,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: lila.socket.Socket.SocketVersion,
      streamers: List[lila.user.User.ID]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = rt.fullName,
      moreCss = cssTag("analyse.relay"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lichess.relay=${safeJsonValue(
          Json.obj(
            "relay"    -> data.relay,
            "study"    -> data.study.add("admin" -> isGranted(_.StudyAdmin)),
            "data"     -> data.analysis,
            "i18n"     -> views.html.study.jsI18n(),
            "tagTypes" -> lila.study.PgnTags.typesToString,
            "userId"   -> ctx.userId,
            "chat" -> chatOption.map(c =>
              chat.json(
                c.chat,
                name = trans.chatRoom.txt(),
                timeout = c.timeout,
                writeable = ctx.userId.??(rt.study.canChat),
                public = false,
                resourceId = lila.chat.Chat.ResourceId(s"relay/${c.chat.id}"),
                localMod = ctx.userId.??(rt.study.canContribute)
              )
            ),
            "explorer" -> Json.obj(
              "endpoint"          -> explorerEndpoint,
              "tablebaseEndpoint" -> tablebaseEndpoint
            ),
            "socketUrl"     -> views.html.study.show.socketUrl(rt.study.id.value),
            "socketVersion" -> socketVersion.value
          )
        )}""")
      ),
      chessground = false,
      zoomable = true,
      csp = defaultCsp.withWebAssembly.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = rt.fullName,
          url = s"$netBaseUrl${rt.path}",
          description = shorten(rt.tour.description, 152)
        )
        .some
    )(
      frag(
        main(cls := "analyse"),
        views.html.study.bits.streamers(streamers)
      )
    )
}
