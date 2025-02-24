package lila.recap
package ui

import lila.ui.*
import ScalatagsTemplate.*
import lila.recap.Recap.Availability

final class RecapUi(helpers: Helpers):
  import helpers.*

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
