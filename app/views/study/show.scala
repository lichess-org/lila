package views.html.study

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue
import lila.common.Json.given
import lila.socket.SocketVersion
import lila.socket.SocketVersion.given

import controllers.routes

object show:

  def apply(
      s: lila.study.Study,
      data: lila.study.JsonView.JsData,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: SocketVersion,
      streamers: List[UserId]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = s.name.value,
      moreCss = cssTag("analyse.study"),
      moreJs = frag(
        analyseNvuiTag,
        jsModuleInit(
          "analysisBoard.study",
          Json.obj(
            "study" -> data.study
              .add("admin", isGranted(_.StudyAdmin))
              .add("hideRatings", !ctx.pref.showRatings),
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
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"study/${c.chat.id}"),
                palantir = ctx.userId exists s.isMember,
                localMod = ctx.userId exists s.canContribute
              )
            },
            "socketUrl"     -> socketUrl(s.id),
            "socketVersion" -> socketVersion
          ) ++ views.html.board.bits.explorerAndCevalConfig
        )
      ),
      robots = s.isPublic,
      zoomable = true,
      csp = analysisCsp.withPeer.withWikiBooks.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = s.name.value,
          url = s"$netBaseUrl${routes.Study.show(s.id).url}",
          description = s"A chess study by ${titleNameOrId(s.ownerId)}"
        )
        .some
    )(
      frag(
        main(cls := "analyse"),
        bits.streamers(streamers)
      )
    )

  def socketUrl(id: lila.study.StudyId) = s"/study/$id/socket/v$apiVersion"
