package views.html.event

import lidraughts.api.Context
import lidraughts.app.templating.Environment._
import lidraughts.app.ui.ScalatagsTemplate._
import lidraughts.common.String.html.richText

import controllers.routes

object show {

  def apply(e: lidraughts.event.Event)(implicit ctx: Context) = views.html.base.layout(
    title = e.title,
    moreCss = responsiveCssTag("event"),
    moreJs = jsTag("event-countdown.js"),
    responsive = true
  ) {
      main(cls := "page-small event box box-pad")(
        h1(dataIcon := "", cls := "text")(e.title),
        h2(cls := "headline")(e.headline),
        e.description.map { d =>
          p(cls := "desc")(richText(d))
        },
        if (e.isFinished) p(cls := "desc")("The event is finished.")
        else {
          if (e.isNow) a(href := e.url, cls := "button button-fat")(trans.eventInProgress.frag())
          else ul(cls := "countdown", dataSeconds := ~e.secondsToStart)(
            List("Days", "Hours", "Minutes", "Seconds") map { t =>
              li(span(cls := t.toLowerCase), t)
            }
          )
        }
      )
    }

  private val dataSeconds = attr("data-seconds")
}
