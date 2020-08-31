package views.html.relay

import views.html.base.layout.{ bits => layout }

import lila.app.templating.Environment._
import lila.app.ui.EmbedConfig
import lila.app.ui.ScalatagsTemplate._
import lila.i18n.{ I18nKeys => trans }

object embed {

  import EmbedConfig.implicits._

  def apply(
      r: lila.relay.Relay,
      s: lila.study.Study,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: lila.socket.Socket.SocketVersion,
      streams: List[lila.streamer.Stream]
  )(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp withNonce config.nonce),
          st.headTitle(s"${s.name} ${chapter.name}"),
          layout.pieceSprite(lila.pref.PieceSet.default),
          cssTagWithTheme("analyse.embed", config.bg)
        ),
        body(
          cls := s"highlight ${config.bg} ${config.board}",
          dataDev := netConfig.minifiedAssets.option("true"),
          dataAssetUrl := netConfig.assetBaseUrl,
          dataAssetVersion := assetVersion.value,
          dataTheme := config.bg
        )(
          div(cls := "is2d")(
            main(cls := "analyse")
          )
        )
      )
  )
      title = r.name,
      moreCss = cssTag("analyse.study"),
      moreJs = frag(
        analyseTag,
        analyseNvuiTag,
        embedJsUnsafe(s"""lichess=window.lichess||{};lichess.relay=${safeJsonValue(
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
                writeable = ctx.userId.??(s.canChat),
                public = false,
                resourceId = lila.chat.Chat.ResourceId(s"relay/${c.chat.id}"),
                localMod = ctx.userId.??(s.canContribute)
              )
            ),
            "explorer" -> Json.obj(
              "endpoint"          -> explorerEndpoint,
              "tablebaseEndpoint" -> tablebaseEndpoint
            ),
            "socketUrl"     -> views.html.study.show.socketUrl(s.id.value),
            "socketVersion" -> socketVersion.value
          )
        )}""")
      ),
      chessground = false,
      zoomable = true,
      csp = defaultCsp.withWebAssembly.withTwitch.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = r.name,
          url = s"$netBaseUrl${routes.Relay.show(r.slug, r.id.value).url}",
          description = shorten(r.description, 152)
        )
        .some
    )(
      frag(
        main(cls := "analyse"),
        views.html.study.bits.streamers(streams)
      )
    )

  def notFound(implicit config: EmbedConfig) =
    frag(
      layout.doctype,
      layout.htmlTag(config.lang)(
        head(
          layout.charset,
          layout.viewport,
          layout.metaCsp(basicCsp),
          st.headTitle(s"404 - ${trans.broadcast.broadcastNotFound()}"),
          cssTagWithTheme("analyse.embed", "dark")
        ),
        body(cls := "dark")(
          div(cls := "not-found")(
            h1(trans.broadcast.broadcastNotFound())
          )
        )
      )
    )
}
