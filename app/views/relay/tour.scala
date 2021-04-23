package views.html
package relay

import play.api.libs.json.Json

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.String.html.safeJsonValue

import controllers.routes
import lila.relay.{ Relay, RelayTour }

object tour {

  def show(t: RelayTour, relays: List[Relay.WithTour])(implicit ctx: Context) =
    views.html.base.layout(
      title = t.name,
      moreCss = cssTag("relay.tour")
    ) {
      main(cls := "relay-tour page-small box")(
        div(cls := "box__top")(
          h1(t.name)
        ),
        div(cls := "list")(
          relays.map { views.html.relay.show.widget(_) }
        )
      )
    }
}
