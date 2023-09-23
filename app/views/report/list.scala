package views.html.report

import controllers.routes
import controllers.appeal.routes.{ Appeal as appealRoutes }
import controllers.report.routes.{ Report as reportRoutes }

import lila.app.templating.Environment.{ given, * }
import lila.app.ui.ScalatagsTemplate.{ *, given }
import lila.report.Report.WithSuspect

object list:

  def apply(
      reports: List[lila.report.Report.WithSuspect],
      filter: String,
      scores: lila.report.Room.Scores,
      streamers: Int,
      appeals: Int
  )(using PageContext) =
    layout(filter, scores, streamers, appeals)(
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
            case WithSuspect(r, sus, _) if !r.isComm || isGranted(_.Shadowban) =>
              tr(cls := List("new" -> r.open))(
                td(
                  reportScore(r.score),
                  strong(r.reason.name.capitalize),
                  br,
                  userLink(sus.user, params = "?mod"),
                  br,
                  p(cls := "perfs")(showBestPerfs(sus.perfs, 2)),
                  views.html.user.mod.userMarks(sus.user, none)
                ),
                td(cls := "atoms")(
                  r.bestAtoms(3).map { atom =>
                    div(cls := "atom")(
                      span(cls := "head")(
                        reportScore(atom.score),
                        " ",
                        userIdLink(atom.by.userId.some),
                        " ",
                        momentFromNowOnce(atom.at)
                      ),
                      p(
                        cls := List(
                          "text"  -> true,
                          "large" -> (atom.text.length > 100 || atom.text.linesIterator.size > 3)
                        )
                      )(shorten(atom.text, 200))
                    )
                  },
                  r.atoms.size > 3 option i(cls := "more")("And ", r.atoms.size - 3, " more")
                ),
                td(
                  r.inquiry match
                    case None =>
                      if r.done.isDefined then
                        postForm(action := reportRoutes.inquiry(r.id), cls := "reopen")(
                          submitButton(dataIcon := licon.PlayTriangle, cls := "text button button-metal")(
                            "Reopen"
                          )
                        )
                      else
                        postForm(action := reportRoutes.inquiry(r.id), cls := "inquiry")(
                          submitButton(dataIcon := licon.PlayTriangle, cls := "button button-metal")
                        )
                    case Some(inquiry) =>
                      frag(
                        "Open by ",
                        userIdLink(inquiry.mod.some)
                      )
                )
              )
            case _ => emptyFrag
          }
        )
      )
    )

  private val scoreTag = tag("score")

  def layout(filter: String, scores: lila.report.Room.Scores, streamers: Int, appeals: Int)(
      body: Frag
  )(using ctx: PageContext) =
    views.html.base.layout(
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
                href := reportRoutes.listWithFilter("all"),
                cls  := List("active" -> (filter == "all"))
              )(
                "All",
                scoreTag(scores.highest)
              ),
              ctx.me.soUse {
                lila.report.Room.values
                  .filter(lila.report.Room.isGranted)
                  .map { room =>
                    a(
                      href := reportRoutes.listWithFilter(room.key),
                      cls := List(
                        "active"            -> (filter == room.key),
                        s"room-${room.key}" -> true
                      )
                    )(
                      room.name,
                      scores.get(room).filter(20 <=).map(scoreTag(_))
                    )
                  }
                  .toList
              }: List[Frag],
              (appeals > 0 && isGranted(_.Appeals)) option a(
                href := appealRoutes.queue(),
                cls := List(
                  "new"    -> true,
                  "active" -> (filter == "appeal")
                )
              )(
                countTag(appeals),
                "Appeals"
              ),
              (isGranted(_.Streamers) && streamers > 0) option
                a(href := s"${routes.Streamer.index()}?requests=1", cls := "new")(
                  countTag(streamers),
                  "Streamers"
                )
            )
          ),
          body
        )
      )
    }
