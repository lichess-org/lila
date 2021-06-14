package views.html.tournament

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

object homepageSpotlight {

  def apply(tour: lila.tournament.Tournament)(implicit ctx: Context) = {
    val schedClass = tour.schedule ?? { sched =>
      val invert  = (sched.freq.isWeeklyOrBetter && tour.isNowOrSoon) ?? " invert"
      val distant = tour.isDistant ?? " distant little"
      s"${sched.freq} ${sched.speed} ${sched.variant.key}$invert$distant"
    }
    val tourClass = s"tour-spotlight id_${tour.id} $schedClass"
    tour.spotlight map { spot =>
      a(href := routes.Tournament.show(tour.id), cls := tourClass)(
        frag(
          spot.iconImg map { i =>
            img(cls := "img", src := assetUrl(s"images/$i"))
          } getOrElse {
            spot.iconFont.fold[Frag](iconTag("")(cls := "img")) {
              case "" => img(cls := "img icon", src := assetUrl(s"images/globe.svg"))
              case i    => iconTag(i)(cls := "img")
            }
          },
          span(cls := "content")(
            span(cls := "name")(tour.name()),
            if (tour.isDistant) span(cls := "more")(momentFromNow(tour.startsAt))
            else
              frag(
                span(cls := "headline")(spot.headline),
                span(cls := "more")(
                  trans.nbPlayers.plural(tour.nbPlayers, tour.nbPlayers.localize),
                  " • ",
                  if (tour.isStarted) trans.finishesX(momentFromNow(tour.finishesAt))
                  else momentFromNow(tour.startsAt)
                )
              )
          )
        )
      )
    } getOrElse a(href := routes.Tournament.show(tour.id), cls := s"little $tourClass")(
      iconTag(tour.perfType.iconChar)(cls := "img"),
      span(cls := "content")(
        span(cls := "name")(tour.name()),
        span(cls := "more")(
          trans.nbPlayers.plural(tour.nbPlayers, tour.nbPlayers.localize),
          " • ",
          if (tour.isStarted) trans.eventInProgress() else momentFromNow(tour.startsAt)
        )
      )
    )
  }
}
