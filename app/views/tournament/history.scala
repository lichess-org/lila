package views.html.tournament

import controllers.routes

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.common.paginator.Paginator
import lila.tournament.Schedule.Freq
import lila.tournament.Tournament

object history:

  def apply(freq: Freq, pager: Paginator[Tournament])(using PageContext) =
    views.html.base.layout(
      title = "Tournament history",
      moreJs = infiniteScrollTag,
      moreCss = cssTag("tournament.history")
    ) {
      main(cls := "page-menu arena-history")(
        views.html.site.bits.pageMenuSubnav(
          allFreqs.map { f =>
            a(cls := freq.name.active(f.name), href := routes.Tournament.history(f.name))(
              nameOf(f)
            )
          }
        ),
        div(cls := "page-menu__content box")(
          boxTop(h1(nameOf(freq), " tournaments")),
          div(cls := "arena-list")(
            table(cls := "slist slist-pad")(
              tbody(cls := "infinite-scroll")(
                pager.currentPageResults map finishedList.apply,
                pagerNextTable(pager, p => routes.Tournament.history(freq.name, p).url)
              )
            )
          )
        )
      )
    }

  private def nameOf(f: Freq) = if f == Freq.Weekend then "Elite" else f.name

  private val allFreqs = List(
    Freq.Unique,
    Freq.Marathon,
    Freq.Shield,
    Freq.Yearly,
    Freq.Monthly,
    Freq.Weekend,
    Freq.Weekly,
    Freq.Daily,
    Freq.Eastern,
    Freq.Hourly
  )
