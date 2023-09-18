package views.html
package relay

import play.api.libs.json.Json

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.*
import lila.common.String.html.safeJsonValue
import lila.common.Json.given
import lila.socket.SocketVersion
import lila.socket.SocketVersion.given

object show:

  def apply(
      rt: lila.relay.RelayRound.WithTourAndStudy,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: SocketVersion,
      streamers: List[UserId]
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = rt.fullName,
      moreCss = cssTag("analyse.relay"),
      moreJs = frag(
        analyseNvuiTag,
        jsModuleInit(
          "analysisBoard.study",
          Json.obj(
            "relay"    -> data.relay,
            "study"    -> data.study.add("admin" -> isGranted(_.StudyAdmin)),
            "data"     -> data.analysis,
            "i18n"     -> bits.jsI18n,
            "tagTypes" -> lila.study.PgnTags.typesToString,
            "userId"   -> ctx.userId,
            "chat" -> chatOption.map(c =>
              chat.json(
                c.chat,
                name = trans.chatRoom.txt(),
                timeout = c.timeout,
                writeable = ctx.userId.so(rt.study.canChat),
                public = true,
                resourceId = lila.chat.Chat.ResourceId(s"relay/${c.chat.id}"),
                localMod = rt.tour.tier.isEmpty && ctx.userId.so(rt.study.canContribute),
                broadcastMod = rt.tour.tier.isDefined && isGranted(_.BroadcastTimeout)
              )
            ),
            "socketUrl"     -> views.html.study.show.socketUrl(rt.study.id),
            "socketVersion" -> socketVersion
          ) ++ views.html.board.bits.explorerAndCevalConfig
        )
      ),
      zoomable = true,
      csp = analysisCsp.withWikiBooks.some,
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
