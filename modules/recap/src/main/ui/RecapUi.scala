package lila.recap
package ui

import play.api.libs.json.Json
import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.recap.Recap.Availability
import lila.common.Json.given
import lila.ui.HtmlHelper.spinner

final class RecapUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.perfStat as tps

  private def title(user: User)(using ctx: Context) =
    if ctx.is(user) then "Your yearly recap"
    else s"${user.username} yearly recap"

  def home(av: Availability, user: User)(using Context) =
    val data = av match
      case Availability.Available(data) => data
      case Availability.Queued(data)    => data
    Page(title(user))
      .css("recap")
      .js(esmInit("recap", data))
      .csp(_.withInlineIconFont): // swiper's `data: font`
        main(cls := "recap"):
          div(id := "recap-swiper", cls := "swiper")
