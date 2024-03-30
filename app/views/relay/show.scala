package views.html
package relay

import play.api.libs.json.Json

import lila.app.templating.Environment.{ *, given }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.Json.given
import lila.core.socket.SocketVersion

object show:

  def apply(
      rt: lila.relay.RelayRound.WithTourAndStudy,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[lila.chat.UserChat.Mine],
      socketVersion: SocketVersion,
      crossSiteIsolation: Boolean = true
  )(using ctx: PageContext) =
    views.html.base.layout(
      title = rt.fullName,
      moreCss = cssTag("analyse.relay"),
      moreJs = analyseNvuiTag,
      pageModule = PageModule(
        "analysisBoard.study",
        Json
          .obj(
            "relay"    -> data.relay,
            "study"    -> data.study.add("admin" -> isGranted(_.StudyAdmin)),
            "data"     -> data.analysis,
            "i18n"     -> bits.jsI18n,
            "tagTypes" -> lila.study.PgnTags.typesToString,
            "userId"   -> ctx.userId,
            "chat" -> chatOption.map: c =>
              chat
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
                ),
            "socketUrl"     -> views.html.study.show.socketUrl(rt.study.id),
            "socketVersion" -> socketVersion
          ) ++ views.html.board.bits.explorerAndCevalConfig
      ).some,
      zoomable = true,
      csp = (if crossSiteIsolation then analysisCsp else defaultCsp).withExternalAnalysisApis.some,
      openGraph = lila.app.ui
        .OpenGraph(
          title = rt.fullName,
          url = s"$netBaseUrl${rt.path}",
          description = shorten(rt.tour.description, 152)
        )
        .some
    ):
      main(cls := "analyse is-relay has-relay-tour")(
        div(cls := "box relay-tour")(
          div(cls := "relay-tour__header")(
            div(cls := "relay-tour__header__content")(
              h1(data.group.fold(rt.tour.name.value)(_.value)),
              div(cls := "relay-tour__header__selectors"):
                div(cls := "mselect relay-tour__mselect"):
                  label(cls := "mselect__label"):
                    span(cls := "relay-tour__round-select__name")(rt.relay.name)
            ),
            div(cls := "relay-tour__header__image"):
              rt.tour.image.map: imgId =>
                img(src := views.html.relay.tour.thumbnail.url(imgId, _.Size.Large), alt := "loading...")
          )
        ),
        st.aside(cls := "relay-tour__side")(div(cls := "relay-tour__side__preload"))
      )
