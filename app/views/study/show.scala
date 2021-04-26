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
      streamers: List[lila.user.User.ID]
  )(implicit ctx: Context) =
    views.html.base.layout(
      title = s.name.value,
      moreCss = cssTag("analyse.study"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lichess.study=${safeJsonValue(
          Json.obj(
            "study"    -> data.study.add("admin" -> isGranted(_.StudyAdmin)),
            "data"     -> data.analysis,
            "i18n"     -> jsI18n(),
            "tagTypes" -> lila.study.PgnTags.typesToString,
            "userId"   -> ctx.userId,
            "chat" -> chatOption.map { c =>
              views.html.chat.json(
                c.chat,
                name = trans.chatRoom.txt(),
                timeout = c.timeout,
                writeable = ctx.userId exists s.canChat,
                public = false,
                resourceId = lila.chat.Chat.ResourceId(s"study/${c.chat.id}"),
                palantir = ctx.userId exists s.isMember,
                localMod = ctx.userId exists s.canContribute
              )
            },
            "explorer" -> Json.obj(
              "endpoint"          -> explorerEndpoint,
              "tablebaseEndpoint" -> tablebaseEndpoint
            ),
            "socketUrl"     -> socketUrl(s.id.value),
            "socketVersion" -> socketVersion.value
          )
        )}""")
      ),
      robots = s.isPublic,
      chessground = false,
      zoomable = true,
      csp = defaultCsp.withWebAssembly.withPeer.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s.name.value,
          url = s"$netBaseUrl${routes.Study.show(s.id.value).url}",
          description = s"A chess study by ${usernameOrId(s.ownerId)}"
        )
        .some
    )(
      frag(
        main(cls := "analyse"),
        bits.streamers(streamers)
      )
    )

  def socketUrl(id: String) = s"/study/$id/socket/v$apiVersion"
}
