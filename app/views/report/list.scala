package views.html.report

import lila.api.Context
import lila.app.templating.Environment._
import lila.app.ui.ScalatagsTemplate._
import lila.report.Report.WithSuspect

import controllers.routes

object list {

  def apply(
    reports: List[lila.report.Report.WithSuspect],
    filter: String,
    counts: lila.report.Room.Counts,
    streamers: Int
  )(implicit ctx: Context) = views.html.base.layout(
    title = "Reports",
    moreCss = cssTag("mod.report")
  ) {
      main(cls := "page-menu")(
        views.html.mod.menu("report"),
        div(id := "report_list", cls := "page-menu__content box")(
          div(cls := "header")(
            i(cls := "icon"),
            span(cls := "tabs")(
              a(
                href := routes.Report.listWithFilter("all"),
                cls := List("new" -> (counts.sum > 0), "active" -> (filter == "all"))
              )(
                  countTag(counts.sum > 0 option counts.sum),
                  "All"
                ),
              lila.report.Room.all.map { room =>
                a(
                  href := routes.Report.listWithFilter(room.key),
                  cls := List(
                    "new" -> counts.value.contains(room),
                    "active" -> (filter == room.key),
                    s"room-${room.key}" -> true
                  )
                )(
                    countTag(counts.get(room)),
                    room.name
                  )
              },
              streamers > 0 option
                a(href := s"${routes.Streamer.index()}?requests=1", cls := "new")(
                  countTag(streamers),
                  "Streamers"
                )
            )
          ),
          table(cls := "slist slist-pad see")(
            thead(
              tr(
                th("Report"),
                th("By"),
                th
              )
            ),
            tbody(
              reports.map {
                case WithSuspect(r, sus, _) if !r.isTrollOrInsult || isGranted(_.Shadowban) =>
                  tr(cls := List("new" -> r.open))(
                    td(
                      reportScore(r.score),
                      strong(r.reason.name.capitalize),
                      br,
                      userLink(sus.user, params = "?mod"),
                      br,
                      p(cls := "perfs")(showBestPerfs(sus.user, 2)),
                      views.html.user.mod.userMarks(sus.user, none)
                    ),
                    td(cls := "atoms")(
                      r.bestAtoms(3).map { atom =>
                        div(cls := "atom")(
                          span(cls := "head")(
                            reportScore(atom.score),
                            " ", userIdLink(atom.by.value.some),
                            " ", momentFromNowOnce(atom.at)
                          ),
                          p(cls := List(
                            "text" -> true,
                            "large" -> (atom.text.size > 100 || atom.text.lines.size > 3)
                          ))(shorten(atom.text, 200))
                        )
                      },
                      r.atoms.size > 3 option i(cls := "more")("And ", (r.atoms.size - 3), " more")
                    ),
                    td(
                      r.processedBy map { u =>
                        st.form(action := routes.Report.inquiry(r.id), method := "post", cls := "reopen")(
                          button(tpe := "submit", dataIcon := "G", cls := "text button button-metal")("Re-open")
                        )
                      } getOrElse st.form(action := routes.Report.inquiry(r.id), method := "post", cls := "inquiry")(
                        button(tpe := "submit", dataIcon := "G", cls := "button button-metal")
                      ),
                      st.form(action := routes.Report.process(r.id), method := "post", cls := "cancel")(
                        button(tpe := "submit", cls := "button button-thin button-empty")("Dismiss")
                      )
                    )
                  )
                case _ => emptyFrag
              }
            )
          )
        )
      )
    }
}
