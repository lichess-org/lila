package views.html.tournament

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }

object homepageSpotlight:

  def apply(tour: lila.tournament.Tournament)(using PageContext) =
    val schedClass = tour.schedule.so: sched =>
      val invert  = (sched.freq.isWeeklyOrBetter && tour.isNowOrSoon) so " invert"
      val distant = tour.isDistant so " distant little"
      s"${sched.freq} ${sched.speed} ${sched.variant.key}$invert$distant"
    val tourClass = s"tour-spotlight id_${tour.id} $schedClass"
    tour.spotlight map { spot =>
      a(href := routes.Tournament.show(tour.id), cls := tourClass)(
        frag(
          spot.iconImg map { i =>
            img(cls := "img", src := assetUrl(s"images/$i"))
          } getOrElse {
            spot.iconFont.fold[Frag](iconTag(licon.Trophy)(cls := "img")) {
              case licon.Globe => img(cls := "img icon", src := assetUrl(s"images/globe.svg"))
              case i           => iconTag(i)(cls := "img")
            }
          },
          span(cls := "content")(
            span(cls := "name")(tour.name()),
            if tour.isDistant then span(cls := "more")(momentFromNow(tour.startsAt))
            else
              frag(
                span(cls := "headline")(spot.headline),
                span(cls := "more")(
                  trans.nbPlayers.plural(tour.nbPlayers, tour.nbPlayers.localize),
                  " • ",
                  if tour.isStarted then trans.finishesX(momentFromNow(tour.finishesAt))
                  else momentFromNow(tour.startsAt)
                )
              )
          )
        )
      )
    } getOrElse a(href := routes.Tournament.show(tour.id), cls := s"little $tourClass")(
      iconTag(tour.perfType.icon)(cls := "img"),
      span(cls := "content")(
        span(cls := "name")(
          tour.name(),
          tour.isTeamRelated option
            iconTag(licon.Group)(
              cls   := "tour-team-icon",
              title := tour.conditions.teamMember.fold(trans.team.teamBattle.txt())(_.teamName)
            )
        ),
        span(cls := "more")(
          trans.nbPlayers.plural(tour.nbPlayers, tour.nbPlayers.localize),
          " • ",
          if tour.isStarted then trans.eventInProgress() else momentFromNow(tour.startsAt)
        )
      )
    )
