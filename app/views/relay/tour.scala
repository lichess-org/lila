package views.html
package relay

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.relay.{ RelayRound, RelayTour }

object tour {

  def show(t: RelayTour, rounds: List[RelayRound.WithTour], markup: Option[Frag])(implicit ctx: Context) =
    views.html.base.layout(
      title = t.name,
      moreCss = cssTag("relay.index")
    ) {
      main(cls := "relay-tour page-small box")(
        div(cls := "box__top")(
          h1(t.name),
          div(cls := "box__top__actions")(
            a(href := routes.RelayTour.edit(t.id.value), cls := "button button-empty")(trans.edit()),
            a(
              href := routes.RelayRound.create(t.id.value),
              cls := "button text",
              dataIcon := "O"
            )(trans.broadcast.addRound())
          )
        ),
        standardFlash(cls := "box__pad"),
        div(cls := "relay-tour__description")(markup getOrElse frag(t.description)),
        div(
          if (rounds.isEmpty) div(cls := "relay-tour__no-round")("No round has been scheduled yet.")
          else
            rounds.map { rt =>
              div(
                cls := List(
                  "relay-widget"         -> true,
                  "relay-widget--active" -> (rt.round.startedAt.isDefined && !rt.round.finished)
                ),
                dataIcon := ""
              )(
                a(cls := "overlay", href := rt.path),
                div(
                  h2(rt.round.name),
                  div(
                    p(rt.tour.description),
                    if (rt.round.finished) trans.finished().some
                    else if (rt.round.startedAt.isDefined) strong(trans.playingRightNow()).some
                    else rt.round.startsAt.map(momentFromNow(_))
                  )
                )
              )
            }
        )
      )
    }

  def url(t: RelayTour) = routes.RelayTour.show(t.slug, t.id.value)
}
