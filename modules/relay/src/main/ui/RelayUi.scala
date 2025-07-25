package lila.relay
package ui

import play.api.libs.json.*

import lila.common.Json.given
import lila.core.id.ImageId
import lila.ui.*
import lila.relay.RelayRound.WithTourAndStudy
import lila.core.socket.SocketVersion

import ScalatagsTemplate.{ *, given }

final class RelayUi(helpers: Helpers)(
    picfitUrl: lila.core.misc.PicfitUrl,
    socketUrl: StudyId => String,
    explorerAndCevalConfig: Context ?=> JsObject
):
  import helpers.{ *, given }

  def broadcastH1 = h1(dataIcon := Icon.RadioTower, cls := "text")

  def show(
      rt: WithTourAndStudy,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[(JsObject, Frag)],
      socketVersion: SocketVersion
  )(using ctx: Context) =
    Page(rt.fullName)
      .css("analyse.relay")
      .i18n(_.study, _.broadcast)
      .i18nOpt(ctx.blind, _.keyboardMove, _.nvui)
      .js(analyseNvuiTag)
      .js(pageModule(rt, data, chatOption, socketVersion))
      .flag(_.zoom)
      .graph(
        OpenGraph(
          title = rt.fullName,
          url = s"$netBaseUrl${rt.path}",
          description = shorten(rt.tour.info.toString, 152),
          image = rt.tour.image.map(thumbnail.url(_, _.Size.Large))
        )
      ):
        showPreload(rt, data)

  def pageModule(
      rt: WithTourAndStudy,
      data: lila.relay.JsonView.JsData,
      chatOption: Option[(JsObject, Frag)],
      socketVersion: SocketVersion,
      embed: Boolean = false
  )(using ctx: Context) =
    PageModule(
      "analyse.study",
      Json
        .obj(
          "relay" -> data.relay,
          "study" -> data.study.add("admin" -> Granter.opt(_.StudyAdmin)),
          "data" -> data.analysis,
          "tagTypes" -> lila.study.PgnTags.typesToString,
          "userId" -> ctx.userId,
          "chat" -> chatOption.map(_._1),
          "socketUrl" -> socketUrl(rt.study.id),
          "socketVersion" -> socketVersion
        )
        .add("embed" -> embed) ++ explorerAndCevalConfig
    )

  def showPreload(rt: WithTourAndStudy, data: lila.relay.JsonView.JsData): Tag =
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
              img(src := thumbnail.url(imgId, _.Size.Large))
        )
      ),
      st.aside(cls := "relay-tour__side")(div(cls := "relay-tour__side__preload"))
    )

  object thumbnail:
    def apply(image: Option[ImageId], size: RelayTour.thumbnail.SizeSelector): Tag =
      image.fold(fallback): id =>
        img(
          cls := "relay-image",
          widthA := size(RelayTour.thumbnail).width,
          heightA := size(RelayTour.thumbnail).height,
          src := url(id, size)
        )
    def fallback = iconTag(Icon.RadioTower)(cls := "relay-image--fallback")
    def url(id: ImageId, size: RelayTour.thumbnail.SizeSelector) =
      RelayTour.thumbnail(picfitUrl, id, size)

  def spotlight(trs: List[RelayCard])(using ctx: Context): List[Tag] =
    trs
      .filter:
        _.tour.spotlight.map(_.language).exists(ctx.acceptLanguages)
      .map(spotlight)

  def spotlight(tr: RelayCard)(using Translate): Tag =
    a(
      href := tr.path,
      cls := s"tour-spotlight event-spotlight relay-spotlight id_${tr.tour.id}"
    )(
      i(cls := "img", dataIcon := Icon.RadioTower),
      span(cls := "content")(
        span(cls := "name")(tr.tour.spotlight.flatMap(_.title) | tr.tour.name.value),
        span(cls := "more")(
          tr.display.caption.fold(tr.display.name.value)(_.value),
          " • ",
          if tr.display.hasStarted
          then trans.site.eventInProgress()
          else tr.display.startsAtTime.map(momentFromNow(_)) | "Soon"
        )
      )
    )

  def howToUse(using Translate) =
    a(dataIcon := Icon.InfoCircle, cls := "text", href := routes.RelayTour.help)(
      trans.broadcast.howToUseLichessBroadcasts()
    )
