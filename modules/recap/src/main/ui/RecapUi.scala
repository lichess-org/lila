package lila.recap
package ui

import play.api.libs.json.Json
import lila.ui.*
import ScalatagsTemplate.{ *, given }
import lila.recap.Recap.Availability
import lila.recap.RecapJson.given
import lila.common.Json.given
import lila.ui.HtmlHelper.spinner

final class RecapUi(helpers: Helpers):
  import helpers.{ *, given }
  import trans.perfStat as tps

  private def title(user: User)(using ctx: Context) =
    if ctx.is(user) then "Your yearly recap"
    else s"${user.username} yearly recap"

  def home(av: Availability, user: User)(using Context) = av match
    case Availability.Available(data) =>
      Page(title(user))
        .csp(_.withInlineIconFont) // swiper's `data: font`
        .js(esmInit("recap", data))
        .css("recap"):
          main(cls := "recap"):
            div(id := "recap-swiper", cls := "swiper")
    case _ =>
      Page(title(user))
        .js(esmInit("recap"))
        .css("recap"):
          main(cls := "recap"):
            div(id := "recap-swiper", cls := "swiper")(
              h1("Hi, ", userSpan(user)),
              p("Did you have a great year playing chess?"),
              spinner
            )
