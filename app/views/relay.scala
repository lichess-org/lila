package views.relay

import lila.app.UiEnv.{ *, given }
import lila.relay.RelayRound.WithTourAndStudy
import lila.core.socket.SocketVersion

val ui = lila.relay.ui.RelayUi(helpers)(
  picfitUrl,
  views.study.socketUrl,
  views.analyse.ui.explorerAndCevalConfig
)
private val card = lila.relay.ui.RelayCardUi(helpers, ui)
val menu = lila.relay.ui.RelayMenuUi(helpers)
val tour = lila.relay.ui.RelayTourUi(helpers, ui, card, menu)
val form = lila.relay.ui.RelayFormUi(helpers, ui, menu)
val group = lila.relay.ui.RelayGroupUi(ui, card, menu)

def show(
    rt: WithTourAndStudy,
    data: lila.relay.JsonView.JsData,
    chatOption: Option[lila.chat.UserChat.Mine],
    socketVersion: SocketVersion,
    crossSiteIsolation: Boolean = true
)(using ctx: Context) =
  val chat = chatOption.map: c =>
    views.chat
      .json(
        c.chat,
        c.lines,
        name = trans.site.chatRoom.txt(),
        timeout = c.timeout,
        writeable = ctx.userId.exists(rt.study.canChat),
        public = true,
        resourceId = lila.chat.Chat.ResourceId(s"relay/${c.chat.id}"),
        localMod = rt.tour.tier.isEmpty && ctx.userId.exists(rt.study.canContribute),
        broadcastMod = rt.tour.tier.isDefined && isGranted(_.BroadcastTimeout),
        hostIds = rt.study.members.ids.toList
      ) -> views.chat.frag
  ui.show(rt, data, chat, socketVersion)
    .csp(crossSiteIsolation.so(views.analyse.ui.bits.cspExternalEngine).compose(_.withExternalAnalysisApis))

def embed(
    rt: WithTourAndStudy,
    data: lila.relay.JsonView.JsData,
    socketVersion: SocketVersion
)(using ctx: EmbedContext) =
  views.base.embed.site(
    title = rt.fullName,
    cssKeys = List("analyse.relay.embed"),
    pageModule = ui.pageModule(rt, data, none, socketVersion, embed = true).some,
    csp = _.withExternalAnalysisApis,
    i18nModules = List(_.site, _.timeago, _.study, _.broadcast)
  )(
    div(id := "main-wrap", cls := "is2d"):
      ui.showPreload(rt, data)(cls := "relay-embed")
    ,
    views.base.page.ui.inlineJs(ctx.nonce, List(Esm("site").some))
  )
