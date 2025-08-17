package lila.relay
package ui

import lila.ui.*
import ScalatagsTemplate.{ *, given }

final class RelayCardUi(helpers: Helpers, ui: RelayUi):
  import helpers.{ *, given }

  private def link(t: RelayTour, url: String, live: Boolean) = a(
    href := url,
    cls := List(
      "relay-card" -> true,
      "relay-card--active" -> t.active,
      "relay-card--live" -> live
    )
  )
  private def image(t: RelayTour) = t.image.fold(ui.thumbnail.fallback(cls := "relay-card__image")): id =>
    img(cls := "relay-card__image", src := ui.thumbnail.url(id, _.Size.Small))

  private def truncatedPlayers(t: RelayTour): Option[Frag] =
    t.info.players.map: players =>
      span(cls := "relay-card__players"):
        players.split(',').map(name => span(name.trim))

  def render[A <: RelayRound.AndTourAndGroup](
      tr: A,
      live: A => Boolean,
      crowd: Crowd,
      alt: Option[RelayRound.WithTour] = None,
      errors: List[String] = Nil
  )(using Context) =
    link(tr.tour, tr.path, live(tr))(
      image(tr.tour),
      span(cls := "relay-card__body")(
        span(cls := "relay-card__info")(
          tr.tour.active.option:
            span(cls := "relay-card__round")(
              tr.display.name,
              (tr.group, alt).mapN: (group, alt) =>
                frag(" & ", group.shortTourName(alt.tour.name))
            )
          ,
          if live(tr)
          then
            span(cls := "relay-card__live")(
              "LIVE",
              crowd.value.some
                .filter(_ > 2)
                .map: nb =>
                  span(cls := "relay-card__crowd text", dataIcon := Icon.User)(nb.localize)
            )
          else tr.display.startedAt.orElse(tr.display.startsAtTime).map(momentFromNow)
        ),
        h3(cls := "relay-card__title")(tr.group.fold(tr.tour.name.value)(_.value)),
        if errors.nonEmpty
        then ul(cls := "relay-card__errors")(errors.map(li(_)))
        else truncatedPlayers(tr.tour)
      )
    )

  def renderCalendar(tr: RelayTour.WithFirstRound)(using Context) =
    link(tr.tour, tr.path, false)(cls := s"relay-card--tier-${tr.tour.tier.so(_.v)}")(
      tr.tour.tier.exists(_ >= RelayTour.Tier.high).option(image(tr.tour)),
      span(cls := "relay-card__body")(
        span(cls := "relay-card__info")(
          tr.display.startedAt
            .orElse(tr.display.startsAtTime)
            .map: date =>
              span(showDate(date))
        ),
        h3(cls := "relay-card__title")(tr.group.fold(tr.tour.name.value)(_.value)),
        truncatedPlayers(tr.tour)
      )
    )

  def renderTourOfGroup(group: RelayGroup)(tour: RelayTour)(using Context) =
    link(tour, routes.RelayTour.show(tour.slug, tour.id).url, false)(
      cls := s"relay-card--tier-${tour.tier.so(_.v)}"
    )(
      image(tour),
      span(cls := "relay-card__body")(
        span(cls := "relay-card__info")(
          tour.dates.map: dates =>
            span(showDate(dates.start))
        ),
        h3(cls := "relay-card__title")(group.name.shortTourName(tour.name)),
        truncatedPlayers(tour)
      )
    )

  def empty(t: RelayTour) =
    link(t, routes.RelayTour.show(t.slug, t.id).url, false)(
      image(t),
      span(cls := "relay-card__body")(
        h3(cls := "relay-card__title")(t.name),
        span(cls := "relay-card__desc")(t.info.toString)
      )
    )
