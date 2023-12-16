package views.html.relay

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.relay.RelayTour
import play.api.i18n.Lang
import scalatags.Text.TypedTag

object bits:

  def broadcastH1 = h1(dataIcon := licon.RadioTower, cls := "text")

  def spotlight(trs: List[RelayTour.ActiveWithSomeRounds])(using ctx: Context): List[Tag] =
    trs
      .filter:
        _.tour.spotlight.map(_.language).exists(ctx.acceptLanguages)
      .map(spotlight)

  def spotlight(tr: RelayTour.ActiveWithSomeRounds)(using Lang): Tag =
    a(
      href := tr.path,
      cls  := s"tour-spotlight event-spotlight relay-spotlight id_${tr.tour.id}"
    )(
      i(cls := "img", dataIcon := licon.RadioTower),
      span(cls := "content")(
        span(cls := "name")(tr.tour.spotlight.flatMap(_.title) | tr.tour.name),
        span(cls := "more")(
          tr.display.caption.fold(tr.display.name.value)(_.value),
          " • ",
          if tr.display.hasStarted
          then trans.eventInProgress()
          else tr.display.startsAt.map(momentFromNow(_)) | "Soon"
        )
      )
    )

  def howToUse(using Lang) =
    a(dataIcon := licon.InfoCircle, cls := "text", href := routes.RelayTour.help)(
      trans.broadcast.howToUseLichessBroadcasts()
    )

  def jsI18n(using Lang) =
    views.html.study.jsI18n() ++
      i18nJsObject(i18nKeys)

  val i18nKeys =
    import trans.broadcast.*
    List(addRound, broadcastUrl, currentRoundUrl, currentGameUrl, downloadAllRounds, editRoundStudy)
