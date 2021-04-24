package views.html
package relay

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._

import controllers.routes
import lila.relay.{ RelayRound, RelayTour }

object tour {

  def show(t: RelayTour, relays: List[RelayRound.WithTour])(implicit ctx: Context) =
    views.html.base.layout(
      title = t.name,
      moreCss = cssTag("relay.index")
    ) {
      main(cls := "relay-tour page-small box")(
        div(cls := "box__top")(
          h1(t.name),
          a(
            href := routes.RelayRound.create(t.id.value),
            cls := "new button text",
            dataIcon := "O"
          )(trans.broadcast.addRound())
        ),
        div(cls := "list")(
          relays.map { views.html.relay.show.widget(_) }
        )
      )
    }

  def url(t: RelayTour) = routes.RelayTour.show(t.slug, t.id.value)
}
