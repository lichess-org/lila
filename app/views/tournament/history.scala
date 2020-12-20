package views.html.tournament

import controllers.routes

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.common.paginator.Paginator
import lila.tournament.Schedule.Freq
import lila.tournament.Tournament

object history {

  def apply(freq: Freq, pager: Paginator[Tournament])(implicit ctx: Context) =
    views.html.base.layout(
      title = "Tournament history",
      moreJs = infiniteScrollTag,
      moreCss = cssTag("tournament.history")
    ) {
      main(cls := "page-menu arena-history")(
        st.nav(cls := "page-menu__menu subnav")(
          allFreqs.map { f =>
            a(cls := freq.name.active(f.name), href := routes.Tournament.history(f.name))(f.name)
          }
        ),
        div(cls := "page-menu__content box")(
          h1(freq.name, " tournaments"),
          div(cls := "arena-list")(
            table(cls := "slist slist-pad")(
              tbody(cls := "infinitescroll")(
                pagerNextTable(pager, p => routes.Tournament.history(freq.name, p).url),
                pager.currentPageResults map finishedList.apply
              )
            )
          )
        )
      )
    }

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
}
