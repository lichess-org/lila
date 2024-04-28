package lila.relay
package ui

import play.api.libs.json.JsObject
import play.api.data.Form
import scalalib.paginator.Paginator

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class RelayBits(helpers: Helpers)(studyJsI18n: () => helpers.Translate ?=> JsObject):
  import helpers.{ *, given }

  def broadcastH1 = h1(dataIcon := Icon.RadioTower, cls := "text")

  def spotlight(trs: List[RelayTour.ActiveWithSomeRounds])(using ctx: Context): List[Tag] =
    trs
      .filter:
        _.tour.spotlight.map(_.language).exists(ctx.acceptLanguages)
      .map(spotlight)

  def spotlight(tr: RelayTour.ActiveWithSomeRounds)(using Translate): Tag =
    a(
      href := tr.path,
      cls  := s"tour-spotlight event-spotlight relay-spotlight id_${tr.tour.id}"
    )(
      i(cls := "img", dataIcon := Icon.RadioTower),
      span(cls := "content")(
        span(cls := "name")(tr.tour.spotlight.flatMap(_.title) | tr.tour.name.value),
        span(cls := "more")(
          tr.display.caption.fold(tr.display.name.value)(_.value),
          " • ",
          if tr.display.hasStarted
          then trans.site.eventInProgress()
          else tr.display.startsAt.map(momentFromNow(_)) | "Soon"
        )
      )
    )

  def howToUse(using Translate) =
    a(dataIcon := Icon.InfoCircle, cls := "text", href := routes.RelayTour.help)(
      trans.broadcast.howToUseLichessBroadcasts()
    )

  def jsI18n(using Translate) =
    studyJsI18n() ++ i18nJsObject(i18nKeys)

  val i18nKeys =
    import trans.broadcast as trb
    List(
      trb.addRound,
      trb.broadcastUrl,
      trb.currentRoundUrl,
      trb.currentGameUrl,
      trb.downloadAllRounds,
      trb.editRoundStudy
    )
